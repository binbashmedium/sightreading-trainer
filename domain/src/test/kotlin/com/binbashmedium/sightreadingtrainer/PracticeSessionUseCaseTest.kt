package com.binbashmedium.sightreadingtrainer

import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
import com.binbashmedium.sightreadingtrainer.domain.usecase.PracticeSessionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PracticeSessionUseCaseTest {

    private lateinit var useCase: PracticeSessionUseCase

    private val threeNoteExercise = Exercise(
        expectedNotes = listOf(
            listOf(60), // C4
            listOf(62), // D4
            listOf(64)  // E4
        )
    )

    @Before
    fun setUp() {
        useCase = PracticeSessionUseCase()
    }

    @Test
    fun `initial state is null`() = runTest {
        assertNull(useCase.state.value)
    }

    @Test
    fun `startSession initialises state with the given exercise`() = runTest {
        useCase.startSession(threeNoteExercise)

        val state = useCase.state.value
        assertNotNull(state)
        assertEquals(threeNoteExercise, state!!.exercise)
        assertEquals(0, state.score)
        assertEquals(0, state.totalAttempts)
    }

    @Test
    fun `correct note advances exercise index and increments score`() = runTest {
        useCase.startSession(threeNoteExercise)

        val result = useCase.processChord(listOf(NoteEvent(60, 100)))

        assertEquals(MatchResult.Correct, result)
        assertEquals(1, useCase.state.value!!.exercise.currentIndex)
        assertEquals(1, useCase.state.value!!.score)
        assertEquals(1, useCase.state.value!!.totalAttempts)
    }

    @Test
    fun `incorrect note does not advance exercise and does not increment score`() = runTest {
        useCase.startSession(threeNoteExercise)

        val result = useCase.processChord(listOf(NoteEvent(62, 100))) // D4 instead of C4

        assertEquals(MatchResult.Incorrect, result)
        assertEquals(0, useCase.state.value!!.exercise.currentIndex)
        assertEquals(0, useCase.state.value!!.score)
        assertEquals(1, useCase.state.value!!.totalAttempts)
    }

    @Test
    fun `playing all notes correctly completes the exercise`() = runTest {
        useCase.startSession(threeNoteExercise)

        useCase.processChord(listOf(NoteEvent(60, 100)))
        useCase.processChord(listOf(NoteEvent(62, 100)))
        useCase.processChord(listOf(NoteEvent(64, 100)))

        val state = useCase.state.value!!
        assertTrue("Exercise should be complete", state.exercise.isComplete)
        assertEquals(3, state.score)
        assertEquals(3, state.totalAttempts)
    }

    @Test
    fun `resetSession clears the state`() = runTest {
        useCase.startSession(threeNoteExercise)
        useCase.processChord(listOf(NoteEvent(60, 100)))

        useCase.resetSession()

        assertNull(useCase.state.value)
    }

    @Test
    fun `processChord without an active session returns Waiting`() = runTest {
        val result = useCase.processChord(listOf(NoteEvent(60, 100)))
        assertEquals(MatchResult.Waiting, result)
    }

    @Test
    fun `chord exercise advances only after exact match`() = runTest {
        val chordExercise = Exercise(expectedNotes = listOf(listOf(60, 64, 67)))
        useCase.startSession(chordExercise)

        // Wrong chord first
        val wrong = useCase.processChord(listOf(NoteEvent(60, 100), NoteEvent(64, 100)))
        assertEquals(MatchResult.Incorrect, wrong)
        assertFalse(useCase.state.value!!.exercise.isComplete)

        // Correct chord
        val correct = useCase.processChord(
            listOf(NoteEvent(60, 100), NoteEvent(64, 100), NoteEvent(67, 100))
        )
        assertEquals(MatchResult.Correct, correct)
        assertTrue(useCase.state.value!!.exercise.isComplete)
    }
}
