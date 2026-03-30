package com.binbashmedium.sightreadingtrainer

import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseStep
import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import com.binbashmedium.sightreadingtrainer.domain.model.PerformanceInput
import com.binbashmedium.sightreadingtrainer.domain.usecase.PracticeSessionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PracticeSessionUseCaseTest {

    private lateinit var useCase: PracticeSessionUseCase

    private val threeNoteExercise = Exercise(
        steps = listOf(
            ExerciseStep(notes = listOf(60)),
            ExerciseStep(notes = listOf(62)),
            ExerciseStep(notes = listOf(64))
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
        useCase.startSession(threeNoteExercise, sessionDurationSec = 75)

        val state = useCase.state.value
        assertNotNull(state)
        assertEquals(threeNoteExercise, state!!.exercise)
        assertEquals(75, state.sessionDurationSec)
        assertEquals(0, state.score)
        assertEquals(0, state.totalAttempts)
    }

    @Test
    fun `correct note advances exercise index and awards base score`() = runTest {
        useCase.startSession(threeNoteExercise)

        val result = useCase.processChord(listOf(NoteEvent(60, 100)))

        assertEquals(MatchResult.Correct, result)
        assertEquals(1, useCase.state.value!!.exercise.currentIndex)
        assertEquals(10, useCase.state.value!!.score)
        assertEquals(1, useCase.state.value!!.correctNotesCount)
        assertEquals(0, useCase.state.value!!.wrongNotesCount)
        assertEquals(1, useCase.state.value!!.totalAttempts)
        assertEquals(listOf(60), useCase.state.value!!.inputByBeat[0]?.playedNotes)
    }

    @Test
    fun `incorrect note advances exercise but does not change score`() = runTest {
        useCase.startSession(threeNoteExercise)

        val result = useCase.processChord(listOf(NoteEvent(62, 100)))

        assertEquals(MatchResult.Incorrect, result)
        assertEquals(1, useCase.state.value!!.exercise.currentIndex)
        assertEquals(0, useCase.state.value!!.score)
        assertEquals(0, useCase.state.value!!.correctNotesCount)
        assertEquals(1, useCase.state.value!!.wrongNotesCount)
        assertEquals(1, useCase.state.value!!.totalAttempts)
    }

    @Test
    fun `playing all notes correctly completes the exercise`() = runTest {
        useCase.startSession(threeNoteExercise)

        useCase.processChord(listOf(NoteEvent(60, 100)))
        useCase.processChord(listOf(NoteEvent(62, 100)))
        useCase.processChord(listOf(NoteEvent(64, 100)))

        val state = useCase.state.value!!
        assertTrue(state.exercise.isComplete)
        assertTrue(state.score >= 30)
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
        val chordExercise = Exercise(steps = listOf(ExerciseStep(notes = listOf(60, 64, 67))))
        useCase.startSession(chordExercise)

        val wrong = useCase.processChord(listOf(NoteEvent(60, 100), NoteEvent(64, 100)))
        assertEquals(MatchResult.Incorrect, wrong)
        assertTrue(useCase.state.value!!.exercise.isComplete)
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
        useCase.processChord(listOf(NoteEvent(62, 100)))

        assertEquals(MatchResult.Incorrect, useCase.state.value!!.resultByBeat[0])
    }

    @Test
    fun `second correct chord produces non-zero BPM`() = runTest {
        useCase.startSession(threeNoteExercise)
        useCase.processChord(listOf(NoteEvent(60, 100)))
        useCase.processChord(listOf(NoteEvent(62, 100)))

        assertTrue(useCase.state.value!!.bpm > 0f)
    }

    @Test
    fun `fluency bonus raises score above base for fast playing`() = runTest {
        useCase.startSession(threeNoteExercise)
        useCase.processChord(listOf(NoteEvent(60, 100)))
        useCase.processChord(listOf(NoteEvent(62, 100)))

        assertTrue(useCase.state.value!!.score > 20)
    }

    @Test
    fun `pedal action must match expected step`() = runTest {
        val exercise = Exercise(
            steps = listOf(ExerciseStep(notes = listOf(60), pedalAction = PedalAction.PRESS))
        )
        useCase.startSession(exercise)

        val result = useCase.processInput(
            PerformanceInput(
                notes = listOf(NoteEvent(60, 100)),
                pedalAction = PedalAction.PRESS
            )
        )

        assertEquals(MatchResult.Correct, result)
    }

    @Test
    fun `wrong pedal action returns Incorrect`() = runTest {
        val exercise = Exercise(
            steps = listOf(ExerciseStep(notes = listOf(60), pedalAction = PedalAction.RELEASE))
        )
        useCase.startSession(exercise)

        val result = useCase.processInput(
            PerformanceInput(
                notes = listOf(NoteEvent(60, 100)),
                pedalAction = PedalAction.PRESS
            )
        )

        assertEquals(MatchResult.Incorrect, result)
    }

    @Test
    fun `pedal only input does not advance exercise`() = runTest {
        useCase.startSession(threeNoteExercise)

        val result = useCase.processInput(
            PerformanceInput(
                notes = emptyList(),
                pedalAction = PedalAction.PRESS,
                timestamp = 1_000L
            )
        )

        val state = useCase.state.value!!
        assertEquals(MatchResult.Waiting, result)
        assertEquals(0, state.exercise.currentIndex)
        assertEquals(0, state.totalAttempts)
        assertTrue(state.resultByBeat.isEmpty())
        assertTrue(state.inputByBeat.isEmpty())
    }

    @Test
    fun `pedal press before note satisfies expected press step`() = runTest {
        val exercise = Exercise(
            steps = listOf(ExerciseStep(notes = listOf(60), pedalAction = PedalAction.PRESS))
        )
        useCase.startSession(exercise)

        useCase.processInput(
            PerformanceInput(
                notes = emptyList(),
                pedalAction = PedalAction.PRESS,
                timestamp = 1_000L
            )
        )
        val result = useCase.processInput(
            PerformanceInput(
                notes = listOf(NoteEvent(60, 100, timestamp = 1_400L)),
                pedalAction = PedalAction.NONE,
                timestamp = 1_400L
            )
        )

        assertEquals(MatchResult.Correct, result)
        assertTrue(useCase.state.value!!.exercise.isComplete)
    }

    @Test
    fun `pedal release shortly before note satisfies expected release step`() = runTest {
        val exercise = Exercise(
            steps = listOf(ExerciseStep(notes = listOf(60), pedalAction = PedalAction.RELEASE))
        )
        useCase.startSession(exercise)

        useCase.processInput(
            PerformanceInput(
                notes = emptyList(),
                pedalAction = PedalAction.PRESS,
                timestamp = 1_000L
            )
        )
        useCase.processInput(
            PerformanceInput(
                notes = emptyList(),
                pedalAction = PedalAction.RELEASE,
                timestamp = 2_000L
            )
        )
        val result = useCase.processInput(
            PerformanceInput(
                notes = listOf(NoteEvent(60, 100, timestamp = 2_700L)),
                pedalAction = PedalAction.NONE,
                timestamp = 2_700L
            )
        )

        assertEquals(MatchResult.Correct, result)
        assertTrue(useCase.state.value!!.exercise.isComplete)
    }

    @Test
    fun `loadNextExercise keeps session totals and resets beat map`() = runTest {
        useCase.startSession(threeNoteExercise, sessionDurationSec = 60)
        useCase.processChord(listOf(NoteEvent(60, 100)))
        val nextExercise = Exercise(steps = listOf(ExerciseStep(notes = listOf(65))))

        useCase.loadNextExercise(nextExercise)

        val state = useCase.state.value!!
        assertEquals(nextExercise.steps, state.exercise.steps)
        assertEquals(0, state.exercise.currentIndex)
        assertEquals(10, state.score)
        assertEquals(1, state.correctNotesCount)
        assertTrue(state.resultByBeat.isEmpty())
        assertTrue(state.inputByBeat.isEmpty())
    }

    @Test
    fun `session tracks correct and wrong group stats`() = runTest {
        val exercise = Exercise(
            steps = listOf(
                ExerciseStep(notes = listOf(60), contentType = ExerciseContentType.SINGLE_NOTES),
                ExerciseStep(notes = listOf(62), contentType = ExerciseContentType.SINGLE_NOTES)
            )
        )
        useCase.startSession(exercise)

        useCase.processChord(listOf(NoteEvent(60, 100)))
        useCase.processChord(listOf(NoteEvent(64, 100)))

        val state = useCase.state.value!!
        assertEquals(1, state.correctGroupStats["SINGLE_NOTES"])
        assertEquals(1, state.wrongGroupStats["SINGLE_NOTES"])
    }

    @Test
    fun `session tracks note stats including extra played note as wrong`() = runTest {
        val exercise = Exercise(
            steps = listOf(ExerciseStep(notes = listOf(60)))
        )
        useCase.startSession(exercise)

        useCase.processInput(
            PerformanceInput(
                notes = listOf(NoteEvent(60, 100), NoteEvent(69, 100))
            )
        )

        val state = useCase.state.value!!
        assertEquals(1, state.correctNoteStats["C4"])
        assertEquals(1, state.wrongNoteStats["A4"])
    }
}
