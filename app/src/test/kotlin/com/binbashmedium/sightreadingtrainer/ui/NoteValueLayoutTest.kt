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

import com.binbashmedium.sightreadingtrainer.domain.model.NoteValue
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for NoteValue.uiBeatUnits and cumulative beat position logic. */
class NoteValueLayoutTest {

    @Test
    fun `WHOLE note has uiBeatUnits of 8f`() {
        assertEquals(8f, NoteValue.WHOLE.uiBeatUnits, 0f)
    }

    @Test
    fun `HALF note has uiBeatUnits of 4f`() {
        assertEquals(4f, NoteValue.HALF.uiBeatUnits, 0f)
    }

    @Test
    fun `QUARTER note has uiBeatUnits of 2f`() {
        assertEquals(2f, NoteValue.QUARTER.uiBeatUnits, 0f)
    }

    @Test
    fun `EIGHTH note has uiBeatUnits of 1f`() {
        assertEquals(1f, NoteValue.EIGHTH.uiBeatUnits, 0f)
    }

    @Test
    fun `uiBeatUnits equals beats times BEATS_PER_STEP`() {
        NoteValue.entries.forEach { nv ->
            assertEquals(nv.beats * BEATS_PER_STEP, nv.uiBeatUnits, 0.001f)
        }
    }

    @Test
    fun `one measure of WHOLE fills BEATS_PER_MEASURE_UNITS`() {
        val total = NoteValue.WHOLE.uiBeatUnits
        assertEquals(BEATS_PER_MEASURE_UNITS, total, 0f)
    }

    @Test
    fun `two HALFs fill one measure`() {
        val total = NoteValue.HALF.uiBeatUnits * 2
        assertEquals(BEATS_PER_MEASURE_UNITS, total, 0f)
    }

    @Test
    fun `four QUARTERs fill one measure`() {
        val total = NoteValue.QUARTER.uiBeatUnits * 4
        assertEquals(BEATS_PER_MEASURE_UNITS, total, 0f)
    }

    @Test
    fun `eight EIGHTHs fill one measure`() {
        val total = NoteValue.EIGHTH.uiBeatUnits * 8
        assertEquals(BEATS_PER_MEASURE_UNITS, total, 0f)
    }

    @Test
    fun `cumulative beats for all-quarter steps are sequential`() {
        val noteValues = List(8) { NoteValue.QUARTER }
        val positions = computeCumulativeBeats(noteValues)
        assertEquals(8, positions.size)
        assertEquals(0f, positions[0], 0f)
        assertEquals(2f, positions[1], 0f)
        assertEquals(4f, positions[2], 0f)
        assertEquals(6f, positions[3], 0f)
        assertEquals(8f, positions[4], 0f)
    }

    @Test
    fun `cumulative beats for mixed note values are correct`() {
        // WHOLE, HALF, HALF, QUARTER, QUARTER, QUARTER, QUARTER
        val noteValues = listOf(
            NoteValue.WHOLE,
            NoteValue.HALF, NoteValue.HALF,
            NoteValue.QUARTER, NoteValue.QUARTER, NoteValue.QUARTER, NoteValue.QUARTER
        )
        val positions = computeCumulativeBeats(noteValues)
        assertEquals(7, positions.size)
        assertEquals(0f, positions[0], 0f)   // WHOLE at 0
        assertEquals(8f, positions[1], 0f)   // HALF at 8 (after WHOLE=8)
        assertEquals(12f, positions[2], 0f)  // HALF at 12 (after HALF=4)
        assertEquals(16f, positions[3], 0f)  // QUARTER at 16
        assertEquals(18f, positions[4], 0f)  // QUARTER at 18
        assertEquals(20f, positions[5], 0f)  // QUARTER at 20
        assertEquals(22f, positions[6], 0f)  // QUARTER at 22
    }

    @Test
    fun `landscape page 0 shows beats 0 to BEATS_PER_ROW`() {
        val currentBeat = 16f
        val landscapePage = (currentBeat / BEATS_PER_ROW).toInt()
        val landscapeStart = landscapePage * BEATS_PER_ROW
        val landscapeEnd = landscapeStart + BEATS_PER_ROW
        assertEquals(0, landscapePage)
        assertEquals(0f, landscapeStart, 0f)
        assertEquals(32f, landscapeEnd, 0f)
    }

    @Test
    fun `landscape page advances when cursor moves past BEATS_PER_ROW`() {
        val currentBeat = 33f
        val landscapePage = (currentBeat / BEATS_PER_ROW).toInt()
        val landscapeStart = landscapePage * BEATS_PER_ROW
        val landscapeEnd = landscapeStart + BEATS_PER_ROW
        assertEquals(1, landscapePage)
        assertEquals(32f, landscapeStart, 0f)
        assertEquals(64f, landscapeEnd, 0f)
    }

    @Test
    fun `landscape page 3 covers beats 96 to 128`() {
        val currentBeat = 100f
        val landscapePage = (currentBeat / BEATS_PER_ROW).toInt()
        val landscapeStart = landscapePage * BEATS_PER_ROW
        val landscapeEnd = landscapeStart + BEATS_PER_ROW
        assertEquals(3, landscapePage)
        assertEquals(96f, landscapeStart, 0f)
        assertEquals(128f, landscapeEnd, 0f)
    }

    /** Helper mirroring the cumulative-beat logic in toGameState(). */
    private fun computeCumulativeBeats(noteValues: List<NoteValue>): List<Float> {
        var cursor = 0f
        return noteValues.map { nv ->
            val beat = cursor
            cursor += nv.uiBeatUnits
            beat
        }
    }
}
