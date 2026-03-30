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
            assertEquals("Each chord should have exactly 1 note", 1, chord.size)
            assertTrue("Right-hand notes should be >= 60", chord[0] >= 60)
        }
    }

    @Test
    fun `level 1 left hand generates lower notes than right hand`() {
        val leftExercise  = useCase.execute(AppSettings(difficulty = 1, handMode = HandMode.LEFT))
        val rightExercise = useCase.execute(AppSettings(difficulty = 1, handMode = HandMode.RIGHT))

        val leftAvg  = leftExercise.expectedNotes.flatten().average()
        val rightAvg = rightExercise.expectedNotes.flatten().average()
        assertTrue("Left-hand notes should be lower than right-hand notes", leftAvg < rightAvg)
    }

    @Test
    fun `level 2 generates two-note chords`() {
        val exercise = useCase.execute(AppSettings(difficulty = 2))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals("Level 2 chords should have 2 notes", 2, chord.size)
        }
    }

    @Test
    fun `level 3 generates interval pairs`() {
        val exercise = useCase.execute(AppSettings(difficulty = 3))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals("Level 3 chords should have 2 notes", 2, chord.size)
        }
    }

    @Test
    fun `level 4 generates triads`() {
        val exercise = useCase.execute(AppSettings(difficulty = 4))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals("Level 4 chords should have 3 notes", 3, chord.size)
        }
    }

    @Test
    fun `level 5 generates exactly 4 chords each a triad`() {
        val exercise = useCase.execute(AppSettings(difficulty = 5))

        assertEquals("Level 5 should have 4 chords", 4, exercise.expectedNotes.size)
        exercise.expectedNotes.forEach { chord ->
            assertEquals("Each chord should be a triad", 3, chord.size)
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
        val cExercise = useCase.execute(AppSettings(difficulty = 1, handMode = HandMode.RIGHT, musicalKey = 0))
        val gExercise = useCase.execute(AppSettings(difficulty = 1, handMode = HandMode.RIGHT, musicalKey = 7))

        val cNotes = cExercise.expectedNotes.flatten().toSet()
        val gNotes = gExercise.expectedNotes.flatten().toSet()

        // C major right hand: 60–72; G major right hand: 67–79
        assertTrue("C major notes should be in range 60–72", cNotes.all { it in 60..72 })
        assertTrue("G major notes should be in range 67–79", gNotes.all { it in 67..79 })
    }

    @Test
    fun `exercise stores the musical key from settings`() {
        val exercise = useCase.execute(AppSettings(difficulty = 1, musicalKey = 5))
        assertEquals(5, exercise.musicalKey)
    }

    @Test
    fun `two calls with same settings produce same note set but may differ in order`() {
        val settings = AppSettings(difficulty = 1, handMode = HandMode.RIGHT, musicalKey = 0)
        val e1 = useCase.execute(settings).expectedNotes.flatten().toSet()
        val e2 = useCase.execute(settings).expectedNotes.flatten().toSet()
        assertEquals("Same notes regardless of shuffle order", e1, e2)
    }
}
