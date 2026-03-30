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
