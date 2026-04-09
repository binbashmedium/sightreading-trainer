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
import com.binbashmedium.sightreadingtrainer.domain.model.ChordProgression
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseMode
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteAccidental
import com.binbashmedium.sightreadingtrainer.domain.model.NoteValue
import com.binbashmedium.sightreadingtrainer.domain.model.OrnamentType
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import com.binbashmedium.sightreadingtrainer.domain.model.ProgressionExerciseType
import com.binbashmedium.sightreadingtrainer.domain.model.ScaleType
import com.binbashmedium.sightreadingtrainer.domain.usecase.GenerateExerciseUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GenerateExerciseUseCaseTest {

    private val useCase = GenerateExerciseUseCase()
    private val nonArpeggioNonProgressionTypes = listOf(
        ExerciseContentType.SINGLE_NOTES,
        ExerciseContentType.OCTAVES,
        ExerciseContentType.THIRDS,
        ExerciseContentType.FIFTHS,
        ExerciseContentType.SIXTHS,
        ExerciseContentType.TRIADS,
        ExerciseContentType.SEVENTHS,
        ExerciseContentType.NINTHS,
        ExerciseContentType.CLUSTERED_CHORDS
    )

    private fun matchesExpectedShape(contentType: ExerciseContentType, notes: List<Int>): Boolean = when (contentType) {
        ExerciseContentType.SINGLE_NOTES -> notes.size == 1
        ExerciseContentType.OCTAVES,
        ExerciseContentType.THIRDS,
        ExerciseContentType.FIFTHS,
        ExerciseContentType.SIXTHS -> notes.size == 2
        ExerciseContentType.TRIADS -> notes.size == 3
        ExerciseContentType.SEVENTHS -> notes.size == 4
        ExerciseContentType.NINTHS -> notes.size == 5
        ExerciseContentType.CLUSTERED_CHORDS -> notes.size >= 3
        ExerciseContentType.ARPEGGIOS,
        ExerciseContentType.PROGRESSIONS -> true
    }

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
    fun `exercise has exactly DEFAULT_EXERCISE_MEASURES measures worth of beat units`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.TRIADS, ExerciseContentType.SEVENTHS),
                handMode = HandMode.RIGHT,
                selectedKeys = setOf(0)
            )
        )

        // Total beat units = sum of each step's note value in beat units (beats × 2)
        val beatsPerMeasure = 4f // 4/4 time
        val totalBeats = exercise.steps.sumOf { it.noteValue.beats.toDouble() }.toFloat()
        val measuresGenerated = totalBeats / beatsPerMeasure
        assertEquals(GenerateExerciseUseCase.DEFAULT_EXERCISE_MEASURES.toFloat(), measuresGenerated, 0.01f)
    }

    @Test
    fun `each step has a valid NoteValue`() {
        val exercise = useCase.execute(
            AppSettings(exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES), handMode = HandMode.RIGHT)
        )

        assertTrue(exercise.steps.isNotEmpty())
        exercise.steps.forEach { step ->
            assertTrue(step.noteValue in NoteValue.entries)
        }
    }

    @Test
    fun `default note value selection allows all note values`() {
        val allValues = mutableSetOf<NoteValue>()
        repeat(100) { seed ->
            val exercise = useCase.execute(
                AppSettings(exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES), handMode = HandMode.RIGHT),
                random = kotlin.random.Random(seed)
            )
            exercise.steps.forEach { allValues += it.noteValue }
        }
        assertEquals(NoteValue.entries.toSet(), allValues)
    }

    @Test
    fun `selected single note value is always respected`() {
        NoteValue.entries.forEachIndexed { seed, noteValue ->
            val exercise = useCase.execute(
                AppSettings(
                    exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                    handMode = HandMode.RIGHT,
                    selectedNoteValues = setOf(noteValue),
                    selectedKeys = setOf(0)
                ),
                random = Random(seed)
            )

            assertTrue("Expected non-empty steps for $noteValue", exercise.steps.isNotEmpty())
            assertTrue(
                "Found non-$noteValue step when only $noteValue was selected",
                exercise.steps.all { it.noteValue == noteValue }
            )
        }
    }

    @Test
    fun `for each non arpeggio type and note value generation respects selected settings`() {
        nonArpeggioNonProgressionTypes.forEach { contentType ->
            NoteValue.entries.forEachIndexed { seed, noteValue ->
                val exercise = useCase.execute(
                    AppSettings(
                        exerciseTypes = setOf(contentType),
                        handMode = HandMode.RIGHT,
                        selectedNoteValues = setOf(noteValue),
                        selectedKeys = setOf(0)
                    ),
                    random = Random(seed + contentType.ordinal * 100)
                )

                assertTrue("Expected steps for $contentType with $noteValue", exercise.steps.isNotEmpty())
                assertTrue(
                    "Expected all steps to be tagged as $contentType",
                    exercise.steps.all { it.contentType == contentType }
                )
                assertTrue(
                    "Expected all note values to be $noteValue for $contentType",
                    exercise.steps.all { it.noteValue == noteValue }
                )
                assertTrue(
                    "Expected generated notes to match $contentType shape",
                    exercise.steps.all { step -> matchesExpectedShape(contentType, step.notes) }
                )
            }
        }
    }

    @Test
    fun `for arpeggios each selected note value is respected`() {
        NoteValue.entries.forEachIndexed { seed, noteValue ->
            val exercise = useCase.execute(
                AppSettings(
                    exerciseTypes = setOf(ExerciseContentType.ARPEGGIOS),
                    handMode = HandMode.RIGHT,
                    selectedNoteValues = setOf(noteValue),
                    selectedKeys = setOf(0)
                ),
                random = Random(10_000 + seed)
            )

            assertTrue("Expected steps for ARPEGGIOS with $noteValue", exercise.steps.isNotEmpty())
            assertTrue(
                "Expected ARPEGGIOS content type only",
                exercise.steps.all { it.contentType == ExerciseContentType.ARPEGGIOS }
            )
            assertTrue(
                "Expected all note values to be $noteValue for ARPEGGIOS",
                exercise.steps.all { it.noteValue == noteValue }
            )
            assertTrue(
                "Arpeggio steps must remain single-note events",
                exercise.steps.all { it.notes.size == 1 }
            )
        }
    }

    @Test
    fun `for progressions each selected note value is respected`() {
        NoteValue.entries.forEachIndexed { seed, noteValue ->
            val exercise = useCase.execute(
                AppSettings(
                    exerciseMode = ExerciseMode.PROGRESSIONS,
                    handMode = HandMode.RIGHT,
                    selectedNoteValues = setOf(noteValue),
                    selectedProgressions = setOf(ChordProgression.I_IV_V_I),
                    selectedKeys = setOf(0)
                ),
                random = Random(20_000 + seed)
            )

            assertTrue("Expected steps for PROGRESSIONS with $noteValue", exercise.steps.isNotEmpty())
            assertTrue(
                "Expected PROGRESSIONS content type only",
                exercise.steps.all { it.contentType == ExerciseContentType.PROGRESSIONS }
            )
            assertTrue(
                "Expected all note values to be $noteValue for PROGRESSIONS",
                exercise.steps.all { it.noteValue == noteValue }
            )
            assertTrue(
                "Progression-only mode should generate chord steps",
                exercise.steps.all { it.notes.size >= 3 }
            )
        }
    }

    @Test
    fun `each measure contains only one uniform note value pattern`() {
        val exercise = useCase.execute(
            AppSettings(exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES), handMode = HandMode.RIGHT),
            random = kotlin.random.Random(42)
        )

        // Reconstruct measures by consuming beat-budget of 4 beats per measure
        val validPatternSizes = setOf(1, 2, 4, 8) // WHOLE, HALF×2, QUARTER×4, EIGHTH×8
        var measureBudget = 0f
        var measureSteps = mutableListOf<NoteValue>()
        for (step in exercise.steps) {
            measureSteps += step.noteValue
            measureBudget += step.noteValue.beats
            if (measureBudget >= 4f - 0.001f) {
                // Measure complete: all steps must have same noteValue
                assertTrue(measureSteps.toSet().size == 1)
                assertTrue(measureSteps.size in validPatternSizes)
                measureBudget = 0f
                measureSteps = mutableListOf()
            }
        }
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
    fun `selected scale mode constrains generated pitch classes in classic mode`() {
        ScaleType.entries.forEachIndexed { seed, scaleType ->
            val exercise = useCase.execute(
                AppSettings(
                    exerciseMode = ExerciseMode.CLASSIC,
                    exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES, ExerciseContentType.TRIADS),
                    handMode = HandMode.BOTH,
                    selectedKeys = setOf(0),
                    selectedScaleType = scaleType,
                    noteAccidentalsEnabled = false
                ),
                random = Random(30_000 + seed)
            )

            val allowedPitchClasses = scaleType.intervals.map { it % 12 }.toSet()
            assertTrue(
                "Found notes outside ${scaleType.name}: ${exercise.expectedNotes.flatten()}",
                exercise.expectedNotes.flatten().all { ((it % 12) + 12) % 12 in allowedPitchClasses }
            )
        }
    }

    @Test
    fun `selected scale mode constrains generated pitch classes in progression mode`() {
        ScaleType.entries.forEachIndexed { seed, scaleType ->
            val exercise = useCase.execute(
                AppSettings(
                    exerciseMode = ExerciseMode.PROGRESSIONS,
                    handMode = HandMode.RIGHT,
                    selectedKeys = setOf(0),
                    selectedScaleType = scaleType,
                    selectedProgressions = setOf(ChordProgression.I_IV_V_I),
                    progressionExerciseTypes = setOf(ProgressionExerciseType.TRIADS),
                    noteAccidentalsEnabled = false
                ),
                random = Random(31_000 + seed)
            )

            val allowedPitchClasses = scaleType.intervals.map { it % 12 }.toSet()
            assertTrue(
                "Found progression notes outside ${scaleType.name}",
                exercise.expectedNotes.flatten().all { ((it % 12) + 12) % 12 in allowedPitchClasses }
            )
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
                selectedKeys = setOf(0)
            )
        )

        assertTrue(exercise.steps.isNotEmpty())
        assertTrue(exercise.steps.all { it.contentType == ExerciseContentType.TRIADS })
    }

    // ── Progressions ──────────────────────────────────────────────────────────
    @Test
    fun `progressions mode generates triads with three notes per chord by default`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseMode = ExerciseMode.PROGRESSIONS,
                handMode = HandMode.RIGHT,
                selectedKeys = setOf(0),
                selectedProgressions = setOf(ChordProgression.I_IV_V_I),
                progressionExerciseTypes = setOf(ProgressionExerciseType.TRIADS)
            ),
            random = Random(1)
        )

        assertTrue(exercise.steps.isNotEmpty())
        exercise.steps.forEach { step -> assertEquals(3, step.notes.size) }
    }

    @Test
    fun `progressions mode tags all steps with PROGRESSIONS content type`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseMode = ExerciseMode.PROGRESSIONS,
                handMode = HandMode.RIGHT,
                selectedKeys = setOf(0),
                selectedProgressions = setOf(ChordProgression.I_V_VI_IV)
            ),
            random = Random(2)
        )

        assertTrue(exercise.steps.all { it.contentType == ExerciseContentType.PROGRESSIONS })
    }

    @Test
    fun `multiple selected progressions choose one random progression per exercise`() {
        val firstChords = mutableSetOf<List<Int>>()

        (1..40).forEach { seed ->
            val exercise = useCase.execute(
                AppSettings(
                    exerciseMode = ExerciseMode.PROGRESSIONS,
                    handMode = HandMode.RIGHT,
                    selectedKeys = setOf(0),
                    selectedProgressions = setOf(ChordProgression.I_IV_V_I, ChordProgression.II_V_I)
                ),
                forcedKey = 0,
                random = Random(seed)
            )
            assertTrue(exercise.steps.isNotEmpty())
            firstChords += exercise.steps.first().notes
        }

        assertTrue(firstChords.any { it == listOf(60, 64, 67) })
        assertTrue(firstChords.any { it == listOf(62, 65, 69) })
    }

    @Test
    fun `progression mode keeps selected progression order chord by chord`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseMode = ExerciseMode.PROGRESSIONS,
                handMode = HandMode.RIGHT,
                selectedKeys = setOf(0),
                selectedProgressions = setOf(ChordProgression.II_V_I),
                progressionExerciseTypes = setOf(ProgressionExerciseType.TRIADS),
                selectedNoteValues = setOf(NoteValue.QUARTER)
            ),
            forcedKey = 0,
            random = Random(11)
        )

        val expectedCycle = listOf(62, 67, 60) // ii - V - I in C
        val stepRoots = exercise.steps.map { step -> step.notes.minOrNull() ?: -1 }
        stepRoots.forEachIndexed { index, root ->
            assertEquals(expectedCycle[index % expectedCycle.size], root)
        }
    }

    @Test
    fun `progression mode ignores classic exercise types and keeps progression material only`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseMode = ExerciseMode.PROGRESSIONS,
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES, ExerciseContentType.TRIADS),
                handMode = HandMode.RIGHT,
                selectedKeys = setOf(0),
                selectedProgressions = setOf(ChordProgression.I_IV_V_I)
            ),
            forcedKey = 0,
            random = Random(7)
        )

        assertTrue(exercise.steps.isNotEmpty())
        assertTrue(exercise.steps.all { it.contentType == ExerciseContentType.PROGRESSIONS })
    }

    @Test
    fun `progressions with BOTH hand distribute notes across both hands`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseMode = ExerciseMode.PROGRESSIONS,
                handMode = HandMode.BOTH,
                selectedKeys = setOf(0),
                selectedProgressions = setOf(ChordProgression.I_IV_V_I)
            ),
            forcedKey = 0,
            random = Random(3)
        )

        assertTrue(exercise.steps.isNotEmpty())
        exercise.steps.forEach { step ->
            assertTrue(step.notes.any { it < 60 })
            assertTrue(step.notes.any { it >= 60 })
        }
    }

    @Test
    fun `progressions can generate sevenths when sevenths mode option is active`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseMode = ExerciseMode.PROGRESSIONS,
                handMode = HandMode.RIGHT,
                selectedKeys = setOf(0),
                selectedProgressions = setOf(ChordProgression.I_IV_V_I),
                progressionExerciseTypes = setOf(ProgressionExerciseType.SEVENTHS)
            ),
            forcedKey = 0,
            random = Random(4)
        )

        assertTrue(exercise.steps.isNotEmpty())
        assertTrue(exercise.steps.all { it.notes.size == 4 })
    }

    @Test
    fun `progressions can generate ninths when ninths mode option is active`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseMode = ExerciseMode.PROGRESSIONS,
                handMode = HandMode.RIGHT,
                selectedKeys = setOf(0),
                selectedProgressions = setOf(ChordProgression.I_IV_V_I),
                progressionExerciseTypes = setOf(ProgressionExerciseType.NINTHS)
            ),
            forcedKey = 0,
            random = Random(5)
        )

        assertTrue(exercise.steps.isNotEmpty())
        assertTrue(exercise.steps.all { it.notes.size == 5 })
    }

    @Test
    fun `progression mode defaults to triads when no explicit voicing option is selected`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseMode = ExerciseMode.PROGRESSIONS,
                handMode = HandMode.RIGHT,
                selectedKeys = setOf(0),
                selectedProgressions = setOf(ChordProgression.I_IV_V_I),
                progressionExerciseTypes = emptySet()
            ),
            forcedKey = 0,
            random = Random(6)
        )

        assertTrue(exercise.steps.isNotEmpty())
        assertTrue(exercise.steps.all { it.notes.size == 3 })
    }

    @Test
    fun `progressions with arpeggios enabled can mix chord blocks and arpeggios`() {
        val settings = AppSettings(
            exerciseMode = ExerciseMode.PROGRESSIONS,
            handMode = HandMode.RIGHT,
            selectedKeys = setOf(0),
            selectedProgressions = setOf(ChordProgression.I_IV_V_I),
            progressionExerciseTypes = setOf(ProgressionExerciseType.TRIADS, ProgressionExerciseType.ARPEGGIOS)
        )

        val mixed = (1..60)
            .map { seed -> useCase.execute(settings, forcedKey = 0, random = Random(seed)) }
            .firstOrNull { exercise ->
                exercise.steps.any { it.notes.size == 1 } && exercise.steps.any { it.notes.size > 1 }
            }

        assertNotNull(mixed)
    }

    @Test
    fun `progression arpeggios keep label roots in progression order and unlabeled tones inside last labeled chord`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseMode = ExerciseMode.PROGRESSIONS,
                handMode = HandMode.RIGHT,
                selectedKeys = setOf(0),
                selectedProgressions = setOf(ChordProgression.II_V_I),
                progressionExerciseTypes = setOf(ProgressionExerciseType.TRIADS, ProgressionExerciseType.ARPEGGIOS),
                selectedNoteValues = setOf(NoteValue.QUARTER)
            ),
            forcedKey = 0,
            random = Random(12)
        )

        val expectedChordSets = listOf(
            setOf(62, 65, 69), // ii
            setOf(67, 71, 74), // V
            setOf(60, 64, 67)  // I
        )

        val labeled = exercise.steps.filter { it.progressionLabelNotes != null }
        assertTrue(labeled.isNotEmpty())
        labeled.forEachIndexed { index, step ->
            val expected = expectedChordSets[index % expectedChordSets.size]
            assertTrue(step.progressionLabelNotes.orEmpty().all { it in expected })
        }

        var lastLabeledChord: Set<Int>? = null
        exercise.steps.forEach { step ->
            if (step.progressionLabelNotes != null) {
                lastLabeledChord = step.progressionLabelNotes!!.toSet()
                return@forEach
            }
            val expected = lastLabeledChord ?: return@forEach
            assertTrue(step.notes.all { it in expected })
        }
    }

    // ── Note value selection ──────────────────────────────────────────────────

    @Test
    fun `selecting only WHOLE produces an exercise with only whole notes`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                handMode = HandMode.RIGHT,
                selectedNoteValues = setOf(NoteValue.WHOLE)
            ),
            random = Random(10)
        )

        assertTrue(exercise.steps.isNotEmpty())
        assertTrue(exercise.steps.all { it.noteValue == NoteValue.WHOLE })
    }

    @Test
    fun `selecting only HALF produces an exercise with only half notes`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                handMode = HandMode.RIGHT,
                selectedNoteValues = setOf(NoteValue.HALF)
            ),
            random = Random(20)
        )

        assertTrue(exercise.steps.isNotEmpty())
        assertTrue(exercise.steps.all { it.noteValue == NoteValue.HALF })
    }

    @Test
    fun `selecting only QUARTER produces an exercise with only quarter notes`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                handMode = HandMode.RIGHT,
                selectedNoteValues = setOf(NoteValue.QUARTER)
            ),
            random = Random(30)
        )

        assertTrue(exercise.steps.isNotEmpty())
        assertTrue(exercise.steps.all { it.noteValue == NoteValue.QUARTER })
    }

    @Test
    fun `selecting only EIGHTH produces an exercise with only eighth notes`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                handMode = HandMode.RIGHT,
                selectedNoteValues = setOf(NoteValue.EIGHTH)
            ),
            random = Random(40)
        )

        assertTrue(exercise.steps.isNotEmpty())
        assertTrue(exercise.steps.all { it.noteValue == NoteValue.EIGHTH })
    }

    @Test
    fun `with default note value selection eighth notes can appear`() {
        val sawEighth = (0..60).any { seed ->
            val exercise = useCase.execute(
                AppSettings(
                    exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                    handMode = HandMode.RIGHT
                ),
                random = Random(seed)
            )
            exercise.steps.any { it.noteValue == NoteValue.EIGHTH }
        }
        assertTrue("Expected to see eighth notes with default selection", sawEighth)
    }

    @Test
    fun `WHOLE and QUARTER selection produces only those two note values`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                handMode = HandMode.RIGHT,
                selectedNoteValues = setOf(NoteValue.WHOLE, NoteValue.QUARTER)
            ),
            random = Random(55)
        )

        assertTrue(exercise.steps.isNotEmpty())
        exercise.steps.forEach { step ->
            assertTrue(
                "Expected WHOLE or QUARTER but got ${step.noteValue}",
                step.noteValue == NoteValue.WHOLE || step.noteValue == NoteValue.QUARTER
            )
        }
    }

    @Test
    fun `total beats per exercise equals DEFAULT_EXERCISE_MEASURES times beats per measure regardless of note value selection`() {
        listOf(
            setOf(NoteValue.WHOLE),
            setOf(NoteValue.HALF),
            setOf(NoteValue.QUARTER),
            setOf(NoteValue.WHOLE, NoteValue.HALF),
            setOf(NoteValue.WHOLE, NoteValue.HALF, NoteValue.QUARTER)
        ).forEach { noteValues ->
            val exercise = useCase.execute(
                AppSettings(
                    exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                    selectedNoteValues = noteValues
                ),
                random = Random(7)
            )
            val totalBeats = exercise.steps.sumOf { it.noteValue.beats.toDouble() }.toFloat()
            val expectedBeats = GenerateExerciseUseCase.DEFAULT_EXERCISE_MEASURES * GenerateExerciseUseCase.BEATS_PER_MEASURE
            assertEquals("noteValues=$noteValues", expectedBeats, totalBeats, 0.01f)
        }
    }

    // ── Pedal mark alignment tests ────────────────────────────────────────────

    @Test
    fun `pedal marks are placed only at beat-boundary positions`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                pedalEventsEnabled = true
            ),
            random = Random(42)
        )
        // Build cumulative beat positions for each step
        var cursor = 0f
        exercise.steps.forEach { step ->
            val beat = cursor
            if (step.pedalAction != PedalAction.NONE) {
                val isOnBoundary = (beat - beat.toLong()) < 0.01f
                assertTrue(
                    "Pedal action ${step.pedalAction} at beat $beat is not on integer boundary",
                    isOnBoundary
                )
            }
            cursor += step.noteValue.beats
        }
    }

    @Test
    fun `every pedal press is followed by a release`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                pedalEventsEnabled = true
            ),
            random = Random(99)
        )
        val pressCount = exercise.steps.count { it.pedalAction == PedalAction.PRESS }
        val releaseCount = exercise.steps.count { it.pedalAction == PedalAction.RELEASE }
        assertEquals("Every press must have a matching release", pressCount, releaseCount)
    }

    @Test
    fun `no pedal marks when pedal events disabled`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                pedalEventsEnabled = false
            ),
            random = Random(1)
        )
        assertTrue(
            "No pedal marks expected when disabled",
            exercise.steps.none { it.pedalAction != PedalAction.NONE }
        )
    }

    // ── Note range clamping tests ─────────────────────────────────────────────

    @Test
    fun `bass-staff notes are within configured bass range`() {
        // applyNoteRanges uses midi < 60 to identify bass-staff notes.
        // Some left-hand motif notes may exceed 60 and are treated as treble-staff notes.
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                handMode = HandMode.LEFT,
                bassNoteRangeMin = 36,  // C2
                bassNoteRangeMax = 55   // G3
            ),
            random = Random(10)
        )
        var bassNoteSeen = false
        exercise.steps.forEach { step ->
            step.notes.forEach { midi ->
                if (midi < 60) {
                    bassNoteSeen = true
                    assertTrue("Bass note $midi should be >= 36", midi >= 36)
                    assertTrue("Bass note $midi should be <= 55", midi <= 55)
                }
            }
        }
        assertTrue("Expected at least one bass note (midi < 60) in left-hand exercise", bassNoteSeen)
    }

    @Test
    fun `all notes are within configured treble range`() {
        // Right-hand SINGLE_NOTES are generated from rightRoot=60, so all start >= 60.
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                handMode = HandMode.RIGHT,
                trebleNoteRangeMin = 60,  // C4
                trebleNoteRangeMax = 72   // C5
            ),
            random = Random(20)
        )
        exercise.steps.forEach { step ->
            step.notes.forEach { midi ->
                assertTrue("Treble note $midi should be >= 60", midi >= 60)
                assertTrue("Treble note $midi should be <= 72", midi <= 72)
            }
        }
    }

    @Test
    fun `notes default range does not restrict exercise generation`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                handMode = HandMode.RIGHT
            ),
            random = Random(5)
        )
        assertTrue("Exercise should have steps", exercise.steps.isNotEmpty())
    }

    // ── Ornaments tests ───────────────────────────────────────────────────────

    @Test
    fun `no ornaments when selectedOrnaments is empty`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                selectedOrnaments = emptySet()
            ),
            random = Random(1)
        )
        assertTrue(
            "No ornaments expected when selectedOrnaments is empty",
            exercise.steps.all { it.ornament == OrnamentType.NONE }
        )
    }

    @Test
    fun `ornaments are applied when selectedOrnaments is non-empty`() {
        // Use a large number of seeds to ensure ornaments appear (1-in-6 chance)
        var foundOrnament = false
        for (seed in 0..50) {
            val exercise = useCase.execute(
                AppSettings(
                    exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                    selectedOrnaments = setOf(OrnamentType.TRILL, OrnamentType.MORDENT, OrnamentType.TURN)
                ),
                random = Random(seed)
            )
            if (exercise.steps.any { it.ornament != OrnamentType.NONE }) {
                foundOrnament = true
                break
            }
        }
        assertTrue("At least one exercise with seeds 0-50 should have ornaments", foundOrnament)
    }

    @Test
    fun `only selected ornament types appear`() {
        // Only TRILL is selected — no MORDENT or TURN should appear
        var foundOrnament = false
        for (seed in 0..50) {
            val exercise = useCase.execute(
                AppSettings(
                    exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                    selectedOrnaments = setOf(OrnamentType.TRILL)
                ),
                random = Random(seed)
            )
            exercise.steps.forEach { step ->
                assertTrue(
                    "Only TRILL or NONE expected, got ${step.ornament}",
                    step.ornament == OrnamentType.NONE || step.ornament == OrnamentType.TRILL
                )
            }
            if (exercise.steps.any { it.ornament == OrnamentType.TRILL }) {
                foundOrnament = true
            }
        }
        assertTrue("At least one TRILL should appear across seeds 0-50", foundOrnament)
    }

    @Test
    fun `ornaments only on quarter notes or longer`() {
        val exercise = useCase.execute(
            AppSettings(
                exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
                selectedOrnaments = setOf(OrnamentType.TRILL, OrnamentType.MORDENT, OrnamentType.TURN),
                selectedNoteValues = NoteValue.entries.toSet()
            ),
            random = Random(7)
        )
        exercise.steps.forEach { step ->
            if (step.ornament != OrnamentType.NONE) {
                assertTrue(
                    "Ornaments should only appear on quarter notes or longer, not ${step.noteValue}",
                    step.noteValue.beats >= 1f
                )
            }
        }
    }

    @Test
    fun `non-arpeggiation ornaments only applied to single-note steps`() {
        // Use chord-producing exercise types so we get multi-note steps
        for (seed in 0..30) {
            val exercise = useCase.execute(
                AppSettings(
                    exerciseTypes = setOf(ExerciseContentType.TRIADS),
                    selectedOrnaments = setOf(OrnamentType.TRILL, OrnamentType.MORDENT, OrnamentType.TURN)
                ),
                random = Random(seed)
            )
            exercise.steps.forEach { step ->
                if (step.ornament != OrnamentType.NONE) {
                    assertEquals(
                        "Non-arpeggiation ornament ${step.ornament} should only appear on single-note steps",
                        1, step.notes.size
                    )
                }
            }
        }
    }

    @Test
    fun `arpeggiation only applied to chord steps`() {
        for (seed in 0..30) {
            val exercise = useCase.execute(
                AppSettings(
                    exerciseTypes = setOf(ExerciseContentType.TRIADS, ExerciseContentType.SINGLE_NOTES),
                    selectedOrnaments = setOf(OrnamentType.ARPEGGIATION)
                ),
                random = Random(seed)
            )
            exercise.steps.forEach { step ->
                if (step.ornament == OrnamentType.ARPEGGIATION) {
                    assertTrue(
                        "Arpeggiation should only appear on chord steps (notes.size > 1)",
                        step.notes.size > 1
                    )
                }
            }
        }
    }
}
