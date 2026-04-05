// Copyright 2026 BinBashMedium
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.binbashmedium.sightreadingtrainer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binbashmedium.sightreadingtrainer.core.midi.AndroidMidiManager
import com.binbashmedium.sightreadingtrainer.core.midi.ChordDetector
import com.binbashmedium.sightreadingtrainer.data.ExerciseRepository
import com.binbashmedium.sightreadingtrainer.data.SettingsRepository
import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.PracticeState
import com.binbashmedium.sightreadingtrainer.domain.usecase.PracticeSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionResultUi(
    val correctNotes: Int,
    val wrongNotes: Int,
    val bpm: Float,
    val practiceTimeSec: Long,
    val highScore: Int,
    val isNewHighScore: Boolean
) {
    /** Accuracy 0–100 % */
    val accuracy: Int get() {
        val total = correctNotes + wrongNotes
        return if (total == 0) 0 else (correctNotes * 100 / total)
    }
}

/** Composite highscore: accuracy × BPM-factor, scaled to session time. */
internal fun computeHighScore(
    correctNotes: Int,
    wrongNotes: Int,
    bpm: Float,
    practiceTimeSec: Long
): Int {
    val total = correctNotes + wrongNotes
    if (total == 0) return 0
    val accuracy = correctNotes.toFloat() / total
    val bpmFactor = bpm.coerceIn(0f, 240f) / 120f   // 1.0 at 120 BPM
    val timeFactor = (practiceTimeSec.coerceIn(0L, 300L) / 60f).coerceAtLeast(0.1f)
    return (accuracy * bpmFactor * timeFactor * 1000).toInt()
}

internal fun hasSessionTimedOut(state: PracticeState, nowMs: Long): Boolean =
    (nowMs - state.startTimeMs) >= state.sessionDurationSec * 1_000L

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val practiceSessionUseCase: PracticeSessionUseCase,
    private val settingsRepository: SettingsRepository,
    private val exerciseRepository: ExerciseRepository,
    private val midiManager: AndroidMidiManager
) : ViewModel() {

    val practiceState: StateFlow<PracticeState?> = practiceSessionUseCase.state
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val _sessionResult = MutableStateFlow<SessionResultUi?>(null)
    val sessionResult: StateFlow<SessionResultUi?> = _sessionResult.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var chordDetector: ChordDetector? = null
    private var noteCollectorJob: Job? = null
    private var pedalCollectorJob: Job? = null
    private var chordCollectorJob: Job? = null
    private var sessionSettings: AppSettings? = null
    private var sessionKey: Int = 0

    init {
        setupMidi()
    }

    /** Generate a new exercise from current settings and start the session. */
    fun startSession() {
        viewModelScope.launch {
            _isLoading.value = true
            _sessionResult.value = null
            practiceSessionUseCase.resetSession()   // clear stale state immediately
            chordDetector?.reset()
            val settings = settingsRepository.settings.first()
            val exercise = exerciseRepository.generateExercise(settings)
            sessionSettings = settings
            sessionKey = exercise.musicalKey
            practiceSessionUseCase.startSession(exercise, settings.exerciseTimeSec)
            _isLoading.value = false
        }
    }

    /** Called by the UI once Verovio has finished rendering to start the session timer. */
    fun onStaffRendered() {
        val state = practiceSessionUseCase.state.value ?: return
        // Reset the session start time so the timer begins from when notes are visible.
        practiceSessionUseCase.resetStartTime()
    }

    /** Discard the current exercise and generate a fresh one. */
    fun reloadSession() {
        startSession()
    }

    private fun setupMidi() {
        viewModelScope.launch {
            settingsRepository.settings.collect { appSettings ->
                chordDetector?.reset()
                chordDetector = ChordDetector(chordWindowMs = appSettings.chordWindowMs.toLong())
                midiManager.openDevice(appSettings.midiDeviceName.ifEmpty { null })

                noteCollectorJob?.cancel()
                noteCollectorJob = launch {
                    midiManager.noteEvents.collect { noteEvent ->
                        chordDetector?.onNoteOn(noteEvent.midiNote, noteEvent.velocity, noteEvent.timestamp)
                    }
                }

                pedalCollectorJob?.cancel()
                pedalCollectorJob = launch {
                    midiManager.pedalEvents.collect { pedalEvent ->
                        chordDetector?.onPedalChange(pedalEvent.action, pedalEvent.timestamp)
                    }
                }

                chordCollectorJob?.cancel()
                chordCollectorJob = launch {
                    chordDetector!!.chords.collect { input ->
                        if (_sessionResult.value != null) return@collect
                        practiceSessionUseCase.processInput(input, appSettings.timingToleranceMs.toLong())
                        val state = practiceSessionUseCase.state.value ?: return@collect
                        if (!state.exercise.isComplete) {
                            finalizeIfTimedOut()
                            return@collect
                        }

                        if (!hasSessionTimedOut(state, System.currentTimeMillis())) {
                            val baseSettings = sessionSettings ?: appSettings
                            val nextExercise = exerciseRepository.generateExercise(baseSettings, forcedKey = sessionKey)
                            practiceSessionUseCase.loadNextExercise(nextExercise)
                        } else {
                            completeAndPersist(state)
                        }
                    }
                }
            }
        }
    }

    fun finalizeIfTimedOut(nowMs: Long = System.currentTimeMillis()) {
        if (_sessionResult.value != null) return
        val state = practiceSessionUseCase.state.value ?: return
        if (!hasSessionTimedOut(state, nowMs)) return
        viewModelScope.launch { completeAndPersist(state) }
    }

    private suspend fun completeAndPersist(state: PracticeState) {
        if (_sessionResult.value != null) return

        val settings = settingsRepository.settings.first()
        val practiceTimeSec = (System.currentTimeMillis() - state.startTimeMs) / 1000L
        val sessionScore = computeHighScore(
            correctNotes = state.correctNotesCount,
            wrongNotes = state.wrongNotesCount,
            bpm = state.bpm,
            practiceTimeSec = practiceTimeSec
        )
        val highScore = maxOf(settings.highScore, sessionScore)
        val isNewHighScore = sessionScore > settings.highScore
        val updatedSettings = settings.copy(
            highScore = highScore,
            totalCorrectNotes = settings.totalCorrectNotes + state.correctNotesCount,
            totalWrongNotes = settings.totalWrongNotes + state.wrongNotesCount,
            correctGroupStats = settings.correctGroupStats.mergeCounts(state.correctGroupStats),
            wrongGroupStats = settings.wrongGroupStats.mergeCounts(state.wrongGroupStats),
            correctNoteStats = settings.correctNoteStats.mergeCounts(state.correctNoteStats),
            wrongNoteStats = settings.wrongNoteStats.mergeCounts(state.wrongNoteStats)
        )
        _sessionResult.value = SessionResultUi(
            correctNotes = state.correctNotesCount,
            wrongNotes = state.wrongNotesCount,
            bpm = state.bpm,
            practiceTimeSec = practiceTimeSec,
            highScore = highScore,
            isNewHighScore = isNewHighScore
        )
        settingsRepository.save(updatedSettings)
    }

    fun stopSession() {
        practiceSessionUseCase.resetSession()
        _sessionResult.value = null
        chordDetector?.reset()
        noteCollectorJob?.cancel()
        pedalCollectorJob?.cancel()
        chordCollectorJob?.cancel()
        midiManager.close()
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}

private fun Map<String, Int>.mergeCounts(delta: Map<String, Int>): Map<String, Int> {
    if (delta.isEmpty()) return this
    val merged = toMutableMap()
    delta.forEach { (key, value) ->
        if (value > 0) merged[key] = (merged[key] ?: 0) + value
    }
    return merged
}
