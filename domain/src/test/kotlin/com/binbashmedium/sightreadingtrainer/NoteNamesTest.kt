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

package com.binbashmedium.sightreadingtrainer

import com.binbashmedium.sightreadingtrainer.core.util.NoteNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteNamesTest {

    @Test
    fun `fromMidi returns correct name for middle C (60)`() {
        assertEquals("C4", NoteNames.fromMidi(60))
    }

    @Test
    fun `fromMidi returns correct name for A4 (69)`() {
        assertEquals("A4", NoteNames.fromMidi(69))
    }

    @Test
    fun `fromMidi handles sharps correctly`() {
        assertEquals("C#4", NoteNames.fromMidi(61))
        assertEquals("D#4", NoteNames.fromMidi(63))
        assertEquals("F#4", NoteNames.fromMidi(66))
    }

    @Test
    fun `fromMidi handles octave boundaries`() {
        assertEquals("C5", NoteNames.fromMidi(72))
        assertEquals("B3", NoteNames.fromMidi(59))
        assertEquals("C-1", NoteNames.fromMidi(0))
    }

    @Test
    fun `toMidi converts middle C correctly`() {
        assertEquals(60, NoteNames.toMidi("C4"))
    }

    @Test
    fun `toMidi round-trips with fromMidi for full piano range`() {
        for (midi in 21..108) {
            assertEquals("Round-trip failed for MIDI $midi", midi, NoteNames.toMidi(NoteNames.fromMidi(midi)))
        }
    }

    @Test
    fun `isBlackKey returns true for all sharps`() {
        assertTrue(NoteNames.isBlackKey(61)) // C#4
        assertTrue(NoteNames.isBlackKey(63)) // D#4
        assertTrue(NoteNames.isBlackKey(66)) // F#4
        assertTrue(NoteNames.isBlackKey(68)) // G#4
        assertTrue(NoteNames.isBlackKey(70)) // A#4
    }

    @Test
    fun `isBlackKey returns false for all natural notes`() {
        assertFalse(NoteNames.isBlackKey(60)) // C
        assertFalse(NoteNames.isBlackKey(62)) // D
        assertFalse(NoteNames.isBlackKey(64)) // E
        assertFalse(NoteNames.isBlackKey(65)) // F
        assertFalse(NoteNames.isBlackKey(67)) // G
        assertFalse(NoteNames.isBlackKey(69)) // A
        assertFalse(NoteNames.isBlackKey(71)) // B
    }
}
