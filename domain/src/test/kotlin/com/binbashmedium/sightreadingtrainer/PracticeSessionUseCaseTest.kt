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
    fun `correct note advances exercise index and awards base score`() = runTest {
        useCase.startSession(threeNoteExercise)

        val result = useCase.processChord(listOf(NoteEvent(60, 100)))

        assertEquals(MatchResult.Correct, result)
        assertEquals(1, useCase.state.value!!.exercise.currentIndex)
        // First chord: base 10 pts, no fluency bonus (no previous timestamp).
        assertEquals(10, useCase.state.value!!.score)
        assertEquals(1, useCase.state.value!!.totalAttempts)
    }

    @Test
    fun `incorrect note advances exercise but does not change score`() = runTest {
        useCase.startSession(threeNoteExercise)

        val result = useCase.processChord(listOf(NoteEvent(62, 100))) // D4 instead of C4

        assertEquals(MatchResult.Incorrect, result)
        // Wrong notes still advance the cursor so the exercise keeps moving.
        assertEquals(1, useCase.state.value!!.exercise.currentIndex)
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
        // Minimum 30 pts (3 × base 10), fast consecutive calls earn fluency bonuses.
        assertTrue("Score should be at least 30", state.score >= 30)
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
    fun `any played chord advances exercise regardless of correctness`() = runTest {
        val chordExercise = Exercise(expectedNotes = listOf(listOf(60, 64, 67)))
        useCase.startSession(chordExercise)

        // A wrong attempt advances past the chord and completes the exercise.
        val wrong = useCase.processChord(listOf(NoteEvent(60, 100), NoteEvent(64, 100)))
        assertEquals(MatchResult.Incorrect, wrong)
        assertTrue("Exercise should be complete after one attempt (wrong)", useCase.state.value!!.exercise.isComplete)
    }

    @Test
    fun `correct chord stores Correct in resultByBeat`() = runTest {
        useCase.startSession(threeNoteExercise)
        useCase.processChord(listOf(NoteEvent(60, 100)))

        assertEquals(MatchResult.Correct, useCase.state.value!!.resultByBeat[0])
    }

    @Test
    fun `incorrect chord stores Incorrect in resultByBeat`() = runTest {
        useCase.startSession(threeNoteExercise)
        useCase.processChord(listOf(NoteEvent(62, 100))) // wrong note

        assertEquals(MatchResult.Incorrect, useCase.state.value!!.resultByBeat[0])
    }

    @Test
    fun `second correct chord produces non-zero BPM`() = runTest {
        useCase.startSession(threeNoteExercise)
        useCase.processChord(listOf(NoteEvent(60, 100)))
        useCase.processChord(listOf(NoteEvent(62, 100)))

        val bpm = useCase.state.value!!.bpm
        assertTrue("BPM should be positive after two correct chords", bpm > 0f)
    }

    @Test
    fun `fluency bonus raises score above base for fast playing`() = runTest {
        useCase.startSession(threeNoteExercise)
        useCase.processChord(listOf(NoteEvent(60, 100))) // first: no bonus
        useCase.processChord(listOf(NoteEvent(62, 100))) // second: fast → fluency bonus

        // First chord = 10 pts, second chord = 10 + fluencyBonus (> 0 for fast call).
        assertTrue("Score should exceed 20 for fast consecutive playing",
            useCase.state.value!!.score > 20)
    }
}
