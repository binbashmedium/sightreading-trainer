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

import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenSummaryTest {

    @Test
    fun `formatExerciseTypeSummary returns fallback for empty set`() {
        assertEquals("SINGLE NOTES", formatExerciseTypeSummary(emptySet()))
    }

    @Test
    fun `formatExerciseTypeSummary returns joined labels for up to three types`() {
        val summary = formatExerciseTypeSummary(
            setOf(
                ExerciseContentType.SINGLE_NOTES,
                ExerciseContentType.TRIADS,
                ExerciseContentType.PROGRESSIONS
            )
        )

        assertEquals("SINGLE NOTES, TRIADS, PROGRESSIONS", summary)
    }

    @Test
    fun `formatExerciseTypeSummary truncates long type lists`() {
        val summary = formatExerciseTypeSummary(
            setOf(
                ExerciseContentType.SINGLE_NOTES,
                ExerciseContentType.OCTAVES,
                ExerciseContentType.THIRDS,
                ExerciseContentType.FIFTHS,
                ExerciseContentType.PROGRESSIONS
            )
        )

        assertEquals("SINGLE NOTES, OCTAVES, THIRDS +2 more", summary)
    }
}
