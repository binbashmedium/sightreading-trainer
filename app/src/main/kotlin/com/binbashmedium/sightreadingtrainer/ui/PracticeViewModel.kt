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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionResultUi(
    val score: Int,
    val correctNotes: Int,
    val wrongNotes: Int,
    val highScore: Int,
    val isNewHighScore: Boolean
)

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
    private val _sessionResult = MutableStateFlow<SessionResultUi?>(null)
    val sessionResult: StateFlow<SessionResultUi?> = _sessionResult.asStateFlow()

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
            chordDetector?.reset()
            val settings = settingsRepository.settings.first()
            val exercise = exerciseRepository.generateExercise(settings)
            sessionSettings = settings
            sessionKey = exercise.musicalKey
            _sessionResult.value = null
            practiceSessionUseCase.startSession(exercise, settings.exerciseTimeSec)
        }
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
        val highScore = maxOf(settings.highScore, state.score)
        val isNewHighScore = state.score > settings.highScore
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
            score = state.score,
            correctNotes = state.correctNotesCount,
            wrongNotes = state.wrongNotesCount,
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
