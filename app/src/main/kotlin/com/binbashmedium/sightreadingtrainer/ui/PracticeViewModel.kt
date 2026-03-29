package com.binbashmedium.sightreadingtrainer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binbashmedium.sightreadingtrainer.core.midi.AndroidMidiManager
import com.binbashmedium.sightreadingtrainer.core.midi.ChordDetector
import com.binbashmedium.sightreadingtrainer.data.SettingsRepository
import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
import com.binbashmedium.sightreadingtrainer.domain.model.PracticeState
import com.binbashmedium.sightreadingtrainer.domain.usecase.PracticeSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val practiceSessionUseCase: PracticeSessionUseCase,
    private val settingsRepository: SettingsRepository,
    private val midiManager: AndroidMidiManager
) : ViewModel() {

    val practiceState: StateFlow<PracticeState?> = practiceSessionUseCase.state

    private var chordDetector: ChordDetector? = null

    fun startSession(exercise: Exercise) {
        practiceSessionUseCase.startSession(exercise)
        setupMidi()
    }

    private fun setupMidi() {
        viewModelScope.launch {
            val settings = settingsRepository.settings
            settings.collect { appSettings ->
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
