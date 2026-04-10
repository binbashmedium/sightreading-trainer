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

internal data class ParsedMidiInput(
    val noteOnEvents: List<Pair<Int, Int>>,
    val pedalActions: List<PedalAction>
)

/**
 * Parses channel voice MIDI messages from [msg] in range [[offset], [offset] + [count]).
 * Supports NOTE_ON and CC64 (sustain pedal), including packets containing multiple
 * 3-byte MIDI messages.
 */
internal fun parseMidiInput(msg: ByteArray, offset: Int, count: Int): ParsedMidiInput {
    val noteOnEvents = mutableListOf<Pair<Int, Int>>()
    val pedalActions = mutableListOf<PedalAction>()
    val endExclusive = (offset + count).coerceAtMost(msg.size)
    var index = offset
    while (index + 2 < endExclusive) {
        val status = msg[index].toInt() and 0xFF
        val data1 = msg[index + 1].toInt() and 0xFF
        val data2 = msg[index + 2].toInt() and 0xFF
        when (status and 0xF0) {
            0x90 -> {
                if (data2 > 0) noteOnEvents += data1 to data2
            }
            0xB0 -> {
                if (data1 == 64) {
                    val action = if (data2 >= 64) PedalAction.PRESS else PedalAction.RELEASE
                    pedalActions += action
                }
            }
        }
        index += 3
    }
    return ParsedMidiInput(noteOnEvents = noteOnEvents, pedalActions = pedalActions)
}
