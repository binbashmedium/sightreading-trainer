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

package com.binbashmedium.sightreadingtrainer

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteAccidental
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
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
                exerciseLength = 12,
                selectedKeys = setOf(0)
            )
        )

        val displayedNotes = exercise.expectedNotes.sumOf { it.size }
        assertTrue(displayedNotes <= 12)
        assertTrue(displayedNotes > 0)
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
    fun `arpeggios generate broken chord single notes`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.ARPEGGIOS),
                handMode = HandMode.RIGHT,
                exerciseLength = 24,
                selectedKeys = setOf(0)
            )
        )

        assertTrue(exercise.expectedNotes.all { it.size == 1 })
        val notes = exercise.expectedNotes.flatten()
        assertTrue(notes.any { it == 60 || it == 64 || it == 67 || it == 71 })
        assertTrue(notes.any { it > 67 })
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

        assertTrue(cExercise.expectedNotes.flatten().all { it in 60..84 })
        assertTrue(gExercise.expectedNotes.flatten().all { it in 67..91 })
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

    @Test
    fun `forced key is used for regeneration`() {
        val exercise = useCase.execute(
            settings = AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                selectedKeys = setOf(0, 5, 9),
                handMode = HandMode.RIGHT
            ),
            forcedKey = 7
        )
        assertEquals(7, exercise.musicalKey)
    }

    @Test
    fun `disabled accidentals leave all note modifiers empty`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES, ExerciseContentType.CLUSTERED_CHORDS),
                handMode = HandMode.RIGHT,
                exerciseLength = 24,
                noteAccidentalsEnabled = false,
                selectedKeys = setOf(0)
            )
        )

        assertTrue(exercise.steps.all { step -> step.noteAccidentals.all { it == NoteAccidental.NONE } })
        val cMajorPitchClasses = setOf(0, 2, 4, 5, 7, 9, 11)
        assertTrue(exercise.expectedNotes.flatten().all { ((it % 12) + 12) % 12 in cMajorPitchClasses })
    }

    @Test
    fun `enabled accidentals generate only sharp flat or valid natural cancellations`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                handMode = HandMode.RIGHT,
                exerciseLength = 48,
                noteAccidentalsEnabled = true,
                selectedKeys = setOf(0)
            )
        )

        val accidentals = exercise.steps.flatMap { it.noteAccidentals }
        assertTrue(accidentals.any { it == NoteAccidental.SHARP || it == NoteAccidental.FLAT || it == NoteAccidental.NATURAL })

        var seenAltered = false
        accidentals.forEach { accidental ->
            if (accidental == NoteAccidental.SHARP || accidental == NoteAccidental.FLAT) {
                seenAltered = true
            }
            if (accidental == NoteAccidental.NATURAL) {
                assertTrue(seenAltered)
            }
        }
    }

    @Test
    fun `enabled pedal events always include a later release for each press`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES, ExerciseContentType.TRIADS),
                handMode = HandMode.RIGHT,
                exerciseLength = 24,
                pedalEventsEnabled = true,
                selectedKeys = setOf(0)
            )
        )

        val pressIndices = exercise.steps.mapIndexedNotNull { index, step ->
            index.takeIf { step.pedalAction == PedalAction.PRESS }
        }
        val releaseIndices = exercise.steps.mapIndexedNotNull { index, step ->
            index.takeIf { step.pedalAction == PedalAction.RELEASE }
        }

        pressIndices.forEach { pressIndex ->
            assertTrue(releaseIndices.any { it > pressIndex })
        }
    }

    @Test
    fun `generated steps keep source content type`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.TRIADS),
                handMode = HandMode.RIGHT,
                exerciseLength = 12,
                selectedKeys = setOf(0)
            )
        )

        assertTrue(exercise.steps.isNotEmpty())
        assertTrue(exercise.steps.all { it.contentType == ExerciseContentType.TRIADS })
    }
}
