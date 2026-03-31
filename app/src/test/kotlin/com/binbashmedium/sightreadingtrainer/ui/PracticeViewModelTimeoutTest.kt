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

import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseStep
import com.binbashmedium.sightreadingtrainer.domain.model.PracticeState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeViewModelTimeoutTest {

    private val exercise = Exercise(steps = listOf(ExerciseStep(notes = listOf(60))))

    @Test
    fun `hasSessionTimedOut is false before duration boundary`() {
        val state = PracticeState(
            exercise = exercise,
            sessionDurationSec = 60,
            startTimeMs = 1_000L
        )

        assertFalse(hasSessionTimedOut(state, nowMs = 60_999L))
    }

    @Test
    fun `hasSessionTimedOut is true at duration boundary`() {
        val state = PracticeState(
            exercise = exercise,
            sessionDurationSec = 60,
            startTimeMs = 1_000L
        )

        assertTrue(hasSessionTimedOut(state, nowMs = 61_000L))
    }

    @Test
    fun `hasSessionTimedOut is true after duration boundary`() {
        val state = PracticeState(
            exercise = exercise,
            sessionDurationSec = 60,
            startTimeMs = 1_000L
        )

        assertTrue(hasSessionTimedOut(state, nowMs = 61_500L))
    }
}
