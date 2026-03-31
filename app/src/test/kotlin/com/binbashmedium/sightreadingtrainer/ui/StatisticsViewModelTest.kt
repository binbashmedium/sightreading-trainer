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

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsViewModelTest {

    @Test
    fun `toTop5 keeps only highest five entries`() {
        val stats = mapOf(
            "A" to 1,
            "B" to 8,
            "C" to 3,
            "D" to 10,
            "E" to 7,
            "F" to 2
        )

        val top = stats.toTop5(prettyGroupNames = false)

        assertEquals(5, top.size)
        assertEquals("D", top[0].label)
        assertEquals(10, top[0].count)
        assertTrue(top.none { it.label == "A" })
    }

    @Test
    fun `toStatisticsUiState maps group names with spaces`() {
        val settings = AppSettings(
            correctGroupStats = mapOf("SINGLE_NOTES" to 12),
            wrongGroupStats = mapOf("TRIADS" to 5),
            correctNoteStats = mapOf("C4" to 9),
            wrongNoteStats = mapOf("F#4" to 4)
        )

        val state = settings.toStatisticsUiState()

        assertEquals("SINGLE NOTES", state.topCorrectGroups.first().label)
        assertEquals(12, state.topCorrectGroups.first().count)
        assertEquals("TRIADS", state.topWrongGroups.first().label)
        assertEquals("C4", state.topCorrectNotes.first().label)
        assertEquals("F#4", state.topWrongNotes.first().label)
    }
}
