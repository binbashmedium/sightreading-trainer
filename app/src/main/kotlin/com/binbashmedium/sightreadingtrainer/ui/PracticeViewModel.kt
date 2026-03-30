package com.binbashmedium.sightreadingtrainer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binbashmedium.sightreadingtrainer.core.midi.AndroidMidiManager
import com.binbashmedium.sightreadingtrainer.core.midi.ChordDetector
import com.binbashmedium.sightreadingtrainer.data.ExerciseRepository
import com.binbashmedium.sightreadingtrainer.data.SettingsRepository
import com.binbashmedium.sightreadingtrainer.domain.model.PracticeState
import com.binbashmedium.sightreadingtrainer.domain.usecase.PracticeSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val practiceSessionUseCase: PracticeSessionUseCase,
    private val settingsRepository: SettingsRepository,
    private val exerciseRepository: ExerciseRepository,
    private val midiManager: AndroidMidiManager
) : ViewModel() {

    val practiceState: StateFlow<PracticeState?> = practiceSessionUseCase.state

    private var chordDetector: ChordDetector? = null

    init {
        setupMidi()
    }

    /** Generate a new exercise from current settings and start the session. */
    fun startSession() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val exercise = exerciseRepository.generateExercise(settings)
            practiceSessionUseCase.startSession(exercise)
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

                launch {
                    midiManager.noteEvents.collect { noteEvent ->
                        chordDetector?.onNoteOn(noteEvent.midiNote, noteEvent.velocity, noteEvent.timestamp)
                    }
                }

                launch {
                    chordDetector!!.chords.collect { chord ->
                        practiceSessionUseCase.processChord(chord, appSettings.timingToleranceMs.toLong())
                    }
                }
            }
        }
    }

    fun stopSession() {
        practiceSessionUseCase.resetSession()
        chordDetector?.reset()
        midiManager.close()
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}
