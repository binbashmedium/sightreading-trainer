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

package com.binbashmedium.sightreadingtrainer.core.util

object NoteNames {

    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /** Returns the human-readable name for a MIDI note number (e.g. 60 → "C4"). */
    fun fromMidi(midiNote: Int): String {
        val octave = (midiNote / 12) - 1
        val noteName = NOTE_NAMES[midiNote % 12]
        return "$noteName$octave"
    }

    /** Converts a note name (e.g. "C4", "C#4") back to its MIDI number. */
    fun toMidi(note: String): Int {
        val match = Regex("([A-G]#?)(-?\\d+)").matchEntire(note)
            ?: throw IllegalArgumentException("Invalid note name: $note")
        val name = match.groupValues[1]
        val octave = match.groupValues[2].toInt()
        val noteIndex = NOTE_NAMES.indexOf(name)
        check(noteIndex >= 0) { "Unknown note name: $name" }
        return (octave + 1) * 12 + noteIndex
    }

    /** Returns true if the MIDI note corresponds to a black piano key. */
    fun isBlackKey(midiNote: Int): Boolean = when (midiNote % 12) {
        1, 3, 6, 8, 10 -> true
        else -> false
    }
}
