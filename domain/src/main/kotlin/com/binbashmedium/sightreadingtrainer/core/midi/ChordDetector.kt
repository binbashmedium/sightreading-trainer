package com.binbashmedium.sightreadingtrainer.core.midi

import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
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
    private val _chords = MutableSharedFlow<List<NoteEvent>>(extraBufferCapacity = 16)
    val chords: SharedFlow<List<NoteEvent>> = _chords.asSharedFlow()

    private var chordJob: Job? = null

    /** Call this whenever a MIDI note-on event is received. */
    fun onNoteOn(
        midiNote: Int,
        velocity: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val event = NoteEvent(midiNote, velocity, timestamp)
        pendingNotes.add(event)

        chordJob?.cancel()
        chordJob = scope.launch {
            delay(chordWindowMs)
            val chord = pendingNotes.toList()
            pendingNotes.clear()
            if (chord.isNotEmpty()) {
                _chords.emit(chord)
            }
        }
    }

    /** Cancels any pending chord and clears the buffer. */
    fun reset() {
        chordJob?.cancel()
        chordJob = null
        pendingNotes.clear()
    }
}
