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
    fun `level 1 right hand generates single notes in right-hand range`() {
        val exercise = useCase.execute(AppSettings(difficulty = 1, handMode = HandMode.RIGHT))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals("Each chord should have exactly 1 note", 1, chord.size)
            assertTrue("Right-hand notes should be >= 60", chord[0] >= 60)
        }
    }

    @Test
    fun `level 1 left hand generates single notes in left-hand range`() {
        val exercise = useCase.execute(AppSettings(difficulty = 1, handMode = HandMode.LEFT))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals("Each chord should have exactly 1 note", 1, chord.size)
            assertTrue("Left-hand notes should be < 60", chord[0] < 60)
        }
    }

    @Test
    fun `level 2 generates two-note chords (parallel octaves)`() {
        val exercise = useCase.execute(AppSettings(difficulty = 2))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals("Level 2 chords should have 2 notes", 2, chord.size)
        }
    }

    @Test
    fun `level 3 generates interval pairs (thirds)`() {
        val exercise = useCase.execute(AppSettings(difficulty = 3))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals("Level 3 chords should have 2 notes", 2, chord.size)
        }
    }

    @Test
    fun `level 4 generates triads (3 notes)`() {
        val exercise = useCase.execute(AppSettings(difficulty = 4))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals("Level 4 chords should have 3 notes", 3, chord.size)
        }
    }

    @Test
    fun `level 5 generates I-IV-V-I progression (4 chords, each a triad)`() {
        val exercise = useCase.execute(AppSettings(difficulty = 5))

        assertEquals("I-IV-V-I should have 4 chords", 4, exercise.expectedNotes.size)
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
    fun `unknown difficulty falls back to level 1`() {
        val exercise = useCase.execute(AppSettings(difficulty = 99, handMode = HandMode.RIGHT))

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals(1, chord.size)
        }
    }
}
