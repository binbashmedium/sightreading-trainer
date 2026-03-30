package com.binbashmedium.sightreadingtrainer

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.usecase.GenerateExerciseUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateExerciseUseCaseTest {

    private val useCase = GenerateExerciseUseCase()

    @Test
    fun `level 1 right hand generates single notes all in treble range`() {
        val exercise = useCase.execute(AppSettings(difficulty = 1, handMode = HandMode.RIGHT))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals(1, chord.size)
            assertTrue("Right-hand notes should be >= 60", chord[0] >= 60)
        }
    }

    @Test
    fun `level 1 left hand generates lower notes than right hand`() {
        val leftExercise = useCase.execute(AppSettings(difficulty = 1, handMode = HandMode.LEFT))
        val rightExercise = useCase.execute(AppSettings(difficulty = 1, handMode = HandMode.RIGHT))

        val leftAvg = leftExercise.expectedNotes.flatten().average()
        val rightAvg = rightExercise.expectedNotes.flatten().average()
        assertTrue("Left-hand notes should be lower than right-hand notes", leftAvg < rightAvg)
    }

    @Test
    fun `configured exercise length is applied to generated exercises`() {
        val exercise = useCase.execute(
            AppSettings(difficulty = 4, handMode = HandMode.RIGHT, exerciseLength = 12)
        )

        assertEquals(12, exercise.expectedNotes.size)
    }

    @Test
    fun `level 2 left hand stays in lower register`() {
        val exercise = useCase.execute(AppSettings(difficulty = 2, handMode = HandMode.LEFT))

        assertTrue(exercise.expectedNotes.all { chord -> chord.all { it < 60 } })
    }

    @Test
    fun `both hand exercises include notes on both staves`() {
        val exercise = useCase.execute(
            AppSettings(difficulty = 3, handMode = HandMode.BOTH, exerciseLength = 10)
        )

        val notes = exercise.expectedNotes.flatten()
        assertTrue(notes.any { it < 60 })
        assertTrue(notes.any { it >= 60 })
    }

    @Test
    fun `level 4 generates triads`() {
        val exercise = useCase.execute(AppSettings(difficulty = 4))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals(3, chord.size)
        }
    }

    @Test
    fun `level 5 respects configured exercise length`() {
        val exercise = useCase.execute(
            AppSettings(difficulty = 5, handMode = HandMode.RIGHT, exerciseLength = 9)
        )

        assertEquals(9, exercise.expectedNotes.size)
        exercise.expectedNotes.forEach { chord ->
            assertEquals(3, chord.size)
        }
    }

    @Test
    fun `exercise starts at index 0 and is not complete`() {
        val exercise = useCase.execute(AppSettings(difficulty = 1))

        assertEquals(0, exercise.currentIndex)
        assertFalse(exercise.isComplete)
    }

    @Test
    fun `unknown difficulty falls back to level 1 structure`() {
        val exercise = useCase.execute(AppSettings(difficulty = 99, handMode = HandMode.RIGHT))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals(1, chord.size)
        }
    }

    @Test
    fun `key transposition shifts all notes by the key offset`() {
        val cExercise = useCase.execute(
            AppSettings(difficulty = 1, handMode = HandMode.RIGHT, musicalKey = 0)
        )
        val gExercise = useCase.execute(
            AppSettings(difficulty = 1, handMode = HandMode.RIGHT, musicalKey = 7)
        )

        val cNotes = cExercise.expectedNotes.flatten().toSet()
        val gNotes = gExercise.expectedNotes.flatten().toSet()

        assertTrue(cNotes.all { it in 60..72 })
        assertTrue(gNotes.all { it in 67..79 })
    }

    @Test
    fun `exercise stores the musical key and hand mode from settings`() {
        val exercise = useCase.execute(
            AppSettings(difficulty = 1, musicalKey = 5, handMode = HandMode.LEFT)
        )

        assertEquals(5, exercise.musicalKey)
        assertEquals(HandMode.LEFT, exercise.handMode)
    }

    @Test
    fun `two calls with same settings produce same note set but may differ in order`() {
        val settings = AppSettings(difficulty = 1, handMode = HandMode.RIGHT, musicalKey = 0)
        val e1 = useCase.execute(settings).expectedNotes.flatten().toSet()
        val e2 = useCase.execute(settings).expectedNotes.flatten().toSet()
        assertEquals(e1, e2)
    }
}
