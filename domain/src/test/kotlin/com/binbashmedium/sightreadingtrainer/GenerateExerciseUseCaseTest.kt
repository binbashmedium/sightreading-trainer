package com.binbashmedium.sightreadingtrainer

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.usecase.GenerateExerciseUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateExerciseUseCaseTest {

    private val useCase = GenerateExerciseUseCase()

    @Test
    fun `single notes right hand generate treble-range notes`() {
        val exercise = useCase.execute(
            AppSettings(exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES), handMode = HandMode.RIGHT)
        )

        assertTrue(exercise.expectedNotes.isNotEmpty())
        exercise.expectedNotes.forEach { chord ->
            assertEquals(1, chord.size)
            assertTrue(chord[0] >= 60)
        }
    }

    @Test
    fun `single notes left hand generate lower notes than right hand`() {
        val leftExercise = useCase.execute(
            AppSettings(exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES), handMode = HandMode.LEFT)
        )
        val rightExercise = useCase.execute(
            AppSettings(exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES), handMode = HandMode.RIGHT)
        )

        assertTrue(leftExercise.expectedNotes.flatten().average() < rightExercise.expectedNotes.flatten().average())
    }

    @Test
    fun `configured exercise length is applied`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.TRIADS, ExerciseContentType.SEVENTHS),
                handMode = HandMode.RIGHT,
                exerciseLength = 12
            )
        )

        assertEquals(12, exercise.expectedNotes.size)
    }

    @Test
    fun `selected key pool constrains generated key`() {
        repeat(20) {
            val exercise = useCase.execute(
                AppSettings(
                    exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                    handMode = HandMode.RIGHT,
                    selectedKeys = setOf(1, 7)
                )
            )
            assertTrue(exercise.musicalKey in setOf(1, 7))
        }
    }

    @Test
    fun `octaves left hand stay in lower register`() {
        val exercise = useCase.execute(
            AppSettings(exerciseTypes = setOf(ExerciseContentType.OCTAVES), handMode = HandMode.LEFT)
        )

        assertTrue(exercise.expectedNotes.all { chord -> chord.all { it < 60 } })
    }

    @Test
    fun `both hand exercises include notes on both staves`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.THIRDS),
                handMode = HandMode.BOTH,
                exerciseLength = 10
            )
        )

        val notes = exercise.expectedNotes.flatten()
        assertTrue(notes.any { it < 60 })
        assertTrue(notes.any { it >= 60 })
    }

    @Test
    fun `mixed selected types produce mixed content`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES, ExerciseContentType.TRIADS, ExerciseContentType.FIFTHS),
                handMode = HandMode.RIGHT,
                exerciseLength = 20,
                selectedKeys = setOf(0)
            )
        )

        assertTrue(exercise.expectedNotes.any { it.size == 1 })
        assertTrue(exercise.expectedNotes.any { it.size == 2 })
        assertTrue(exercise.expectedNotes.any { it.size == 3 })
    }

    @Test
    fun `sevenths and ninths produce extended chords`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SEVENTHS, ExerciseContentType.NINTHS),
                handMode = HandMode.RIGHT,
                exerciseLength = 18,
                selectedKeys = setOf(0)
            )
        )

        assertTrue(exercise.expectedNotes.any { it.size == 4 })
        assertTrue(exercise.expectedNotes.any { it.size == 5 })
    }

    @Test
    fun `single note material includes fifth based leaps`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                handMode = HandMode.RIGHT,
                exerciseLength = 40,
                selectedKeys = setOf(0)
            )
        )

        val notes = exercise.expectedNotes.flatten().sorted()
        assertTrue(notes.any { it >= 74 })
    }

    @Test
    fun `clustered chords stay within a close position span`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.CLUSTERED_CHORDS),
                handMode = HandMode.RIGHT,
                exerciseLength = 16,
                selectedKeys = setOf(0)
            )
        )

        assertTrue(exercise.expectedNotes.all { chord -> chord.size >= 3 })
        assertTrue(exercise.expectedNotes.all { chord -> ((chord.maxOrNull() ?: 0) - (chord.minOrNull() ?: 0)) <= 12 })
    }

    @Test
    fun `exercise starts at index 0 and is not complete`() {
        val exercise = useCase.execute(AppSettings())

        assertEquals(0, exercise.currentIndex)
        assertFalse(exercise.isComplete)
    }

    @Test
    fun `fixed key selection transposes notes correctly`() {
        val cExercise = useCase.execute(
            AppSettings(exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES), handMode = HandMode.RIGHT, selectedKeys = setOf(0))
        )
        val gExercise = useCase.execute(
            AppSettings(exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES), handMode = HandMode.RIGHT, selectedKeys = setOf(7))
        )

        assertTrue(cExercise.expectedNotes.flatten().all { it in 60..72 })
        assertTrue(gExercise.expectedNotes.flatten().all { it in 67..79 })
    }

    @Test
    fun `exercise stores generated key and hand mode`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                selectedKeys = setOf(5),
                handMode = HandMode.LEFT
            )
        )

        assertEquals(5, exercise.musicalKey)
        assertEquals(HandMode.LEFT, exercise.handMode)
    }
}
