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

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for portrait-mode layout constants and helper functions. */
class PortraitLayoutTest {

    @Test
    fun `constants are self-consistent`() {
        assertEquals(8f, BEATS_PER_MEASURE_UNITS, 0f)
        assertEquals(32f, BEATS_PER_ROW, 0f)
        assertEquals(128f, BEATS_PER_PAGE, 0f)
    }

    @Test
    fun `BEATS_PER_MEASURE_UNITS equals four quarter beats times BEATS_PER_STEP`() {
        assertEquals(4 * BEATS_PER_STEP, BEATS_PER_MEASURE_UNITS, 0f)
    }

    @Test
    fun `BEATS_PER_ROW equals MEASURES_PER_ROW times BEATS_PER_MEASURE_UNITS`() {
        assertEquals(MEASURES_PER_ROW * BEATS_PER_MEASURE_UNITS, BEATS_PER_ROW, 0f)
    }

    @Test
    fun `BEATS_PER_PAGE equals ROWS_PER_PAGE times BEATS_PER_ROW`() {
        assertEquals(ROWS_PER_PAGE * BEATS_PER_ROW, BEATS_PER_PAGE, 0f)
    }

    @Test
    fun `beatToPage returns 0 for beats within first page`() {
        assertEquals(0, beatToPage(0f))
        assertEquals(0, beatToPage(127f))
        assertEquals(0, beatToPage(64f))
    }

    @Test
    fun `beatToPage returns 1 for beats in second page`() {
        assertEquals(1, beatToPage(128f))
        assertEquals(1, beatToPage(255f))
    }

    @Test
    fun `beatToPage returns 2 for beats in third page`() {
        assertEquals(2, beatToPage(256f))
    }

    @Test
    fun `pageStartBeat returns correct start beats`() {
        assertEquals(0f, pageStartBeat(0), 0f)
        assertEquals(128f, pageStartBeat(1), 0f)
        assertEquals(256f, pageStartBeat(2), 0f)
    }

    @Test
    fun `rowMeasureLabel returns 1-based measure number`() {
        // Row 0 starts at beat 0 → measure 1
        assertEquals(1, rowMeasureLabel(0f))
        // Row 1 starts at beat 32 → measure 32/8+1 = 5
        assertEquals(5, rowMeasureLabel(32f))
        // Row 2 starts at beat 64 → measure 64/8+1 = 9
        assertEquals(9, rowMeasureLabel(64f))
        // Row 3 starts at beat 96 → measure 96/8+1 = 13
        assertEquals(13, rowMeasureLabel(96f))
        // Page 1, row 0 starts at beat 128 → measure 128/8+1 = 17
        assertEquals(17, rowMeasureLabel(128f))
    }

    @Test
    fun `portrait page rows cover non-overlapping beat ranges`() {
        val page = 0
        val pageStart = pageStartBeat(page)
        val rowRanges = (0 until ROWS_PER_PAGE).map { rowIdx ->
            val rowStart = pageStart + rowIdx * BEATS_PER_ROW
            val rowEnd = rowStart + BEATS_PER_ROW
            rowStart to rowEnd
        }
        // Verify no overlap: each row starts where the previous ended
        for (i in 1 until rowRanges.size) {
            assertEquals(rowRanges[i - 1].second, rowRanges[i].first, 0f)
        }
        // Full page coverage
        assertEquals(pageStart, rowRanges.first().first, 0f)
        assertEquals(pageStart + BEATS_PER_PAGE, rowRanges.last().second, 0f)
    }

    @Test
    fun `bar lines occur at correct local beat positions`() {
        // For a row with 4 measures of BEATS_PER_MEASURE_UNITS each,
        // bar lines should be at 8, 16, 24, 32 (local beats).
        val beatsPerMeasure = BEATS_PER_MEASURE_UNITS
        val rowBeats = BEATS_PER_ROW
        val expectedBarBeats = listOf(8f, 16f, 24f, 32f)

        val actualBarBeats = mutableListOf<Float>()
        var barLocal = beatsPerMeasure
        while (barLocal <= rowBeats + 0.01f) {
            actualBarBeats.add(barLocal)
            barLocal += beatsPerMeasure
        }
        assertEquals(expectedBarBeats, actualBarBeats)
    }

    @Test
    fun `cursor is in row only when currentBeat is within row range`() {
        val rowStart = 32f
        val rowEnd = 64f

        // Before row
        val beatBefore = 10f
        val inRowBefore = beatBefore >= rowStart && beatBefore < rowEnd
        assertEquals(false, inRowBefore)

        // Start of row
        val beatAtStart = 32f
        val inRowAtStart = beatAtStart >= rowStart && beatAtStart < rowEnd
        assertEquals(true, inRowAtStart)

        // Middle of row
        val beatMiddle = 48f
        val inRowMiddle = beatMiddle >= rowStart && beatMiddle < rowEnd
        assertEquals(true, inRowMiddle)

        // At end (exclusive)
        val beatAtEnd = 64f
        val inRowAtEnd = beatAtEnd >= rowStart && beatAtEnd < rowEnd
        assertEquals(false, inRowAtEnd)

        // After row
        val beatAfter = 80f
        val inRowAfter = beatAfter >= rowStart && beatAfter < rowEnd
        assertEquals(false, inRowAfter)
    }

    @Test
    fun `DEFAULT_EXERCISE_MEASURES covers exactly one portrait page in beats`() {
        // 16 measures × 8 beat-units/measure = 128 beat-units = 1 page
        val expectedMeasures = com.binbashmedium.sightreadingtrainer.domain.usecase.GenerateExerciseUseCase.DEFAULT_EXERCISE_MEASURES
        val pageBeats = expectedMeasures * BEATS_PER_MEASURE_UNITS
        assertEquals(BEATS_PER_PAGE, pageBeats, 0f)
    }
}
