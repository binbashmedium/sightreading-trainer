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

import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import org.junit.Assert.assertEquals
import org.junit.Test

class MidiMessageParserTest {

    @Test
    fun `parseMidiInput parses multiple note on messages in one packet`() {
        val packet = byteArrayOf(
            0x90.toByte(), 60, 100,
            0x90.toByte(), 64, 101,
            0x90.toByte(), 67, 102
        )

        val parsed = parseMidiInput(packet, offset = 0, count = packet.size)

        assertEquals(listOf(60 to 100, 64 to 101, 67 to 102), parsed.noteOnEvents)
        assertEquals(emptyList<PedalAction>(), parsed.pedalActions)
    }

    @Test
    fun `parseMidiInput ignores note on with zero velocity`() {
        val packet = byteArrayOf(
            0x90.toByte(), 60, 0,
            0x90.toByte(), 62, 80
        )

        val parsed = parseMidiInput(packet, offset = 0, count = packet.size)

        assertEquals(listOf(62 to 80), parsed.noteOnEvents)
    }

    @Test
    fun `parseMidiInput parses sustain pedal press and release in same packet`() {
        val packet = byteArrayOf(
            0xB0.toByte(), 64, 127.toByte(),
            0xB0.toByte(), 64, 0
        )

        val parsed = parseMidiInput(packet, offset = 0, count = packet.size)

        assertEquals(listOf(PedalAction.PRESS, PedalAction.RELEASE), parsed.pedalActions)
        assertEquals(emptyList<Pair<Int, Int>>(), parsed.noteOnEvents)
    }

    @Test
    fun `parseMidiInput honors offset and count range`() {
        val packet = byteArrayOf(
            0x00, 0x00, 0x00,
            0x90.toByte(), 65, 70
        )

        val parsed = parseMidiInput(packet, offset = 3, count = 3)

        assertEquals(listOf(65 to 70), parsed.noteOnEvents)
    }
}
