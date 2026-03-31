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

package com.binbashmedium.sightreadingtrainer.core.midi

import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import com.binbashmedium.sightreadingtrainer.domain.model.PerformanceInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Groups MIDI note-on events that arrive within [chordWindowMs] milliseconds of each other
 * into a single chord emission.
 *
 * After the first note in a group arrives, the detector waits [chordWindowMs] before
 * emitting the accumulated notes as a chord. Any additional notes within that window are
 * included in the same chord; a new note after the window starts a fresh chord.
 */
class ChordDetector(
    private val chordWindowMs: Long = 50L,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val pendingNotes = mutableListOf<NoteEvent>()
    private var pendingPedalAction: PedalAction = PedalAction.NONE
    private var pendingTimestamp: Long = 0L
    private val _chords = MutableSharedFlow<PerformanceInput>(extraBufferCapacity = 16)
    val chords: SharedFlow<PerformanceInput> = _chords.asSharedFlow()

    private var chordJob: Job? = null

    /** Call this whenever a MIDI note-on event is received. */
    fun onNoteOn(
        midiNote: Int,
        velocity: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val event = NoteEvent(midiNote, velocity, timestamp)
        pendingNotes.add(event)
        if (pendingTimestamp == 0L) pendingTimestamp = timestamp

        scheduleEmission()
    }

    fun onPedalChange(
        action: PedalAction,
        timestamp: Long = System.currentTimeMillis()
    ) {
        pendingPedalAction = action
        if (pendingTimestamp == 0L) pendingTimestamp = timestamp
        scheduleEmission()
    }

    private fun scheduleEmission() {
        chordJob?.cancel()
        chordJob = scope.launch {
            delay(chordWindowMs)
            val chord = pendingNotes.toList()
            val pedalAction = pendingPedalAction
            val timestamp = pendingTimestamp.takeIf { it > 0L } ?: System.currentTimeMillis()
            pendingNotes.clear()
            pendingPedalAction = PedalAction.NONE
            pendingTimestamp = 0L
            if (chord.isNotEmpty() || pedalAction != PedalAction.NONE) {
                _chords.emit(PerformanceInput(chord, pedalAction, timestamp))
            }
        }
    }

    /** Cancels any pending chord and clears the buffer. */
    fun reset() {
        chordJob?.cancel()
        chordJob = null
        pendingNotes.clear()
        pendingPedalAction = PedalAction.NONE
        pendingTimestamp = 0L
    }
}
