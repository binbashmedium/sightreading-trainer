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

package com.binbashmedium.sightreadingtrainer.domain.usecase

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.ChordProgression
import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseStep
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteAccidental
import com.binbashmedium.sightreadingtrainer.domain.model.NoteValue
import com.binbashmedium.sightreadingtrainer.domain.model.OrnamentType
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import kotlin.random.Random

class GenerateExerciseUseCase {

    private enum class ProgressionChordStyle {
        TRIAD,
        SEVENTH,
        NINTH,
        SUS2,
        SUS4
    }

    companion object {
        val KEY_NAMES = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")
        /** Fixed exercise length: 4 rows × 4 measures = one portrait page. */
        const val DEFAULT_EXERCISE_MEASURES = 16
        /** Internal material pool size: worst case 16 measures × 8 eighths. */
        const val MATERIAL_POOL_SIZE = 128
        /** All valid uniform measure fill patterns for 4/4 time. */
        val MEASURE_PATTERNS: List<List<NoteValue>> = listOf(
            listOf(NoteValue.WHOLE),
            listOf(NoteValue.HALF, NoteValue.HALF),
            listOf(NoteValue.QUARTER, NoteValue.QUARTER, NoteValue.QUARTER, NoteValue.QUARTER),
            listOf(NoteValue.EIGHTH, NoteValue.EIGHTH, NoteValue.EIGHTH, NoteValue.EIGHTH,
                   NoteValue.EIGHTH, NoteValue.EIGHTH, NoteValue.EIGHTH, NoteValue.EIGHTH)
        )
        /** Total beats in one 4/4 measure. */
        const val BEATS_PER_MEASURE = 4f
        /**
         * Minimum gap (in beats) that must remain between the last notehead of a measure
         * and the following bar line.  One quarter note (1f beat) ensures the bar line is
         * never visually adjacent to the last notehead of the measure.
         * The 8×EIGHTH pattern places its last note at beat 3.5 which exceeds the allowed
         * maximum of BEATS_PER_MEASURE - BARLINE_GAP_BEATS = 3f, so that pattern is
         * excluded whenever the gap constraint is active.
         */
        const val BARLINE_GAP_BEATS = 1f

        private val SCALE_7 = listOf(0, 2, 4, 5, 7, 9, 11)
        private val SINGLE_NOTE_MOTION = listOf(0, 2, 4, 7, 5, 9, 11, 12, 7, 4, 9, 5, 11, 14)
        private val THIRDS = listOf(0 to 4, 2 to 5, 4 to 7, 5 to 9, 7 to 11, 9 to 12, 11 to 14)
        private val FIFTHS = listOf(0 to 7, 2 to 9, 4 to 11, 5 to 12, 7 to 14, 9 to 16, 11 to 18)
        private val SIXTHS = listOf(0 to 9, 2 to 11, 4 to 12, 5 to 14, 7 to 16, 9 to 17, 11 to 19)
        private val TRIADS = listOf(
            listOf(0, 4, 7), listOf(2, 5, 9), listOf(4, 7, 11), listOf(5, 9, 12),
            listOf(7, 11, 14), listOf(9, 12, 16), listOf(11, 14, 17)
        )
        private val SEVENTHS = listOf(
            listOf(0, 4, 7, 11), listOf(2, 5, 9, 12), listOf(4, 7, 11, 14), listOf(5, 9, 12, 16),
            listOf(7, 11, 14, 17), listOf(9, 12, 16, 19), listOf(11, 14, 17, 21)
        )
        private val NINTHS = listOf(
            listOf(0, 4, 7, 11, 14), listOf(2, 5, 9, 12, 16),
            listOf(7, 11, 14, 17, 21), listOf(9, 12, 16, 19, 23)
        )
        private val CLUSTERED_CHORDS = listOf(
            listOf(4, 7, 12),      // first inversion major triad
            listOf(3, 7, 12),      // first inversion minor triad
            listOf(7, 12, 16),     // second inversion major triad
            listOf(7, 12, 15),     // second inversion minor triad
            listOf(4, 7, 11, 12),  // close-position major seventh
            listOf(3, 7, 10, 12),  // close-position minor seventh
            listOf(4, 7, 10, 12),  // close-position dominant seventh
            listOf(4, 7, 11, 14),  // clustered major ninth without root doubling
            listOf(3, 7, 10, 14)   // clustered minor ninth shell
        )
        private val ARPEGGIO_CONTOURS = listOf(
            listOf(0, 1, 2, 1),
            listOf(0, 1, 2, 3),
            listOf(0, 1, 2, 3, 2, 1)
        )
    }

    fun execute(
        settings: AppSettings,
        forcedKey: Int? = null,
        random: Random = Random.Default
    ): Exercise {
        val generatedKey = (forcedKey ?: settings.selectedKeys.ifEmpty { setOf(0) }.toList().random(random)).coerceIn(0, 11)
        val rightRoot = 60 + generatedKey
        val leftRoot = 48 + generatedKey
        val selectedTypes = settings.exerciseTypes.ifEmpty { setOf(ExerciseContentType.SINGLE_NOTES) }
        val progressionModifierTypes = setOf(
            ExerciseContentType.ARPEGGIOS,
            ExerciseContentType.TRIADS,
            ExerciseContentType.SEVENTHS,
            ExerciseContentType.NINTHS,
            ExerciseContentType.CLUSTERED_CHORDS
        )

        // Progression steps are ordered (not shuffled) and built separately.
        val progressionSteps = buildProgressionSteps(settings, generatedKey, rightRoot, leftRoot, random)

        // In progression mode, chord-shape types act as progression modifiers rather than separate material.
        val nonProgressionTypes = if (ExerciseContentType.PROGRESSIONS in selectedTypes) {
            selectedTypes - ExerciseContentType.PROGRESSIONS - progressionModifierTypes
        } else {
            selectedTypes - ExerciseContentType.PROGRESSIONS
        }
        // Remaining non-progression types are shuffled as before.
        val shuffledMaterial = buildShuffledMaterial(nonProgressionTypes, settings, generatedKey, rightRoot, leftRoot, random)

        val materialPool = if (progressionSteps.isNotEmpty()) {
            buildMaterialPool(progressionSteps, shuffledMaterial, MATERIAL_POOL_SIZE)
        } else if (shuffledMaterial.isNotEmpty()) {
            buildMaterialPool(emptyList(), shuffledMaterial, MATERIAL_POOL_SIZE)
        } else {
            // Fallback: shouldn't normally happen, but be safe.
            buildMaterialPool(
                emptyList(),
                patternsForType(ExerciseContentType.SINGLE_NOTES, settings.handMode, rightRoot, leftRoot, random),
                MATERIAL_POOL_SIZE
            )
        }

        val mixedExercise = applyMeasurePatterns(materialPool, DEFAULT_EXERCISE_MEASURES, settings.selectedNoteValues.ifEmpty { NoteValue.entries.toSet() }, random)
        val accidentalsApplied = applyGeneratedAccidentals(mixedExercise, settings.noteAccidentalsEnabled)
        val pedalApplied = applyPedalMarks(accidentalsApplied, settings.pedalEventsEnabled)
        val rangeApplied = applyNoteRanges(pedalApplied, settings, generatedKey)
        val ornamentsApplied = applyOrnaments(rangeApplied, settings.selectedOrnaments, random)

        return Exercise(steps = ornamentsApplied, musicalKey = generatedKey, handMode = settings.handMode)
    }

    /**
     * Builds a circular material pool from progression steps (ordered first) and
     * shuffled material, capped at [maxSize].
     */
    private fun buildMaterialPool(
        progressionSteps: List<ExerciseStep>,
        shuffledMaterial: List<ExerciseStep>,
        maxSize: Int
    ): List<ExerciseStep> {
        val combined = progressionSteps + shuffledMaterial
        if (combined.isEmpty()) return emptyList()
        return (0 until maxSize).map { i -> combined[i % combined.size] }
    }

    /**
     * Assigns note values to steps by applying random uniform measure patterns.
     * Each of the [numMeasures] measures gets one randomly chosen pattern from
     * [MEASURE_PATTERNS], filtered to:
     *   1. Only patterns whose note values are all in [selectedNoteValues].
     *   2. Only patterns where the last notehead starts at beat ≤
     *      [BEATS_PER_MEASURE] - [BARLINE_GAP_BEATS] (= 3f), ensuring at least
     *      one quarter-note gap before every bar line.
     * If no pattern satisfies both constraints the gap-only filter is used as
     * fallback (preserving the gap guarantee regardless of note-value selection).
     * Steps are drawn sequentially from [materialPool] (wrapping around).
     */
    private fun applyMeasurePatterns(
        materialPool: List<ExerciseStep>,
        numMeasures: Int,
        selectedNoteValues: Set<NoteValue>,
        random: Random
    ): List<ExerciseStep> {
        if (materialPool.isEmpty()) return emptyList()

        val maxLastBeat = BEATS_PER_MEASURE - BARLINE_GAP_BEATS // 3f

        // Patterns satisfying the bar-line gap constraint (always valid regardless of selection).
        val gapValidPatterns = MEASURE_PATTERNS.filter { pattern ->
            pattern.dropLast(1).sumOf { it.beats.toDouble() }.toFloat() <= maxLastBeat
        }

        // Patterns satisfying both: user selection AND the gap constraint.
        val preferredPatterns = gapValidPatterns.filter { pattern ->
            pattern.all { nv -> nv in selectedNoteValues }
        }

        // Use preferred when available; fall back to all gap-valid patterns otherwise.
        val effectivePatterns = if (preferredPatterns.isNotEmpty()) preferredPatterns else gapValidPatterns

        val result = mutableListOf<ExerciseStep>()
        var poolIndex = 0
        repeat(numMeasures) {
            val pattern = effectivePatterns.random(random)
            for (noteValue in pattern) {
                val step = materialPool[poolIndex % materialPool.size]
                result += step.copy(noteValue = noteValue)
                poolIndex++
            }
        }
        return result
    }

    /** Builds ordered steps for all selected [ChordProgression]s (no shuffle). */
    private fun buildProgressionSteps(
        settings: AppSettings,
        generatedKey: Int,
        rightRoot: Int,
        leftRoot: Int,
        random: Random
    ): List<ExerciseStep> {
        if (ExerciseContentType.PROGRESSIONS !in settings.exerciseTypes) return emptyList()

        val selectedProgs = settings.selectedProgressions.ifEmpty { setOf(ChordProgression.I_IV_V_I) }
        val progression = selectedProgs.toList().random(random)
        val chordStyles = progressionStylesForSettings(settings)
        val arpeggiosEnabled = ExerciseContentType.ARPEGGIOS in settings.exerciseTypes

        val steps = progression.chords.flatMap { triadOffsets ->
            val style = chordStyles.random(random)
            val styledOffsets = progressionOffsetsForStyle(triadOffsets.first(), style)
            val notes = progressionNotesForHandMode(styledOffsets, settings.handMode, rightRoot, leftRoot)
            val renderedSteps = if (arpeggiosEnabled && random.nextBoolean()) {
                arpeggiateProgressionChord(notes, random)
            } else {
                listOf(notes)
            }
            renderedSteps.map { renderedNotes ->
                ExerciseStep(
                    notes = renderedNotes,
                    noteAccidentals = List(renderedNotes.size) { NoteAccidental.NONE },
                    contentType = ExerciseContentType.PROGRESSIONS
                )
            }
        }

        return if (!settings.noteAccidentalsEnabled) {
            steps.map { step ->
                val cn = step.notes.map { constrainNoteToScale(it, generatedKey) }
                step.copy(notes = cn, noteAccidentals = List(cn.size) { NoteAccidental.NONE })
            }
        } else steps
    }

    private fun progressionStylesForSettings(settings: AppSettings): List<ProgressionChordStyle> {
        val styles = mutableListOf<ProgressionChordStyle>()
        val hasExplicitChordType = settings.exerciseTypes.any {
            it == ExerciseContentType.TRIADS ||
                it == ExerciseContentType.SEVENTHS ||
                it == ExerciseContentType.NINTHS ||
                it == ExerciseContentType.CLUSTERED_CHORDS
        }

        if (ExerciseContentType.TRIADS in settings.exerciseTypes || !hasExplicitChordType) {
            styles += ProgressionChordStyle.TRIAD
        }
        if (ExerciseContentType.SEVENTHS in settings.exerciseTypes) {
            styles += ProgressionChordStyle.SEVENTH
        }
        if (ExerciseContentType.NINTHS in settings.exerciseTypes) {
            styles += ProgressionChordStyle.NINTH
        }
        if (ExerciseContentType.CLUSTERED_CHORDS in settings.exerciseTypes) {
            styles += ProgressionChordStyle.SUS2
            styles += ProgressionChordStyle.SUS4
        }

        return if (styles.isNotEmpty()) styles else listOf(ProgressionChordStyle.TRIAD)
    }

    private fun progressionOffsetsForStyle(
        rootOffset: Int,
        style: ProgressionChordStyle
    ): List<Int> = when (style) {
        ProgressionChordStyle.TRIAD -> diatonicStackOffsets(rootOffset, tones = 3)
        ProgressionChordStyle.SEVENTH -> diatonicStackOffsets(rootOffset, tones = 4)
        ProgressionChordStyle.NINTH -> diatonicStackOffsets(rootOffset, tones = 5)
        ProgressionChordStyle.SUS2 -> diatonicSuspendedOffsets(rootOffset, useFourth = false)
        ProgressionChordStyle.SUS4 -> diatonicSuspendedOffsets(rootOffset, useFourth = true)
    }

    private fun diatonicStackOffsets(rootOffset: Int, tones: Int): List<Int> {
        val rootPitchClass = ((rootOffset % 12) + 12) % 12
        val rootScaleIndex = SCALE_7.indexOf(rootPitchClass)
        if (rootScaleIndex < 0) return listOf(rootOffset)

        return (0 until tones).map { toneIndex ->
            val scaleIndex = rootScaleIndex + toneIndex * 2
            val octaveShift = scaleIndex / SCALE_7.size
            SCALE_7[scaleIndex % SCALE_7.size] + octaveShift * 12
        }
    }

    private fun diatonicSuspendedOffsets(rootOffset: Int, useFourth: Boolean): List<Int> {
        val rootPitchClass = ((rootOffset % 12) + 12) % 12
        val rootScaleIndex = SCALE_7.indexOf(rootPitchClass)
        if (rootScaleIndex < 0) return listOf(rootOffset, rootOffset + if (useFourth) 5 else 2, rootOffset + 7)

        fun scaleOffset(step: Int): Int {
            val scaleIndex = rootScaleIndex + step
            val octaveShift = scaleIndex / SCALE_7.size
            return SCALE_7[scaleIndex % SCALE_7.size] + octaveShift * 12
        }

        return if (useFourth) {
            listOf(scaleOffset(0), scaleOffset(3), scaleOffset(4))
        } else {
            listOf(scaleOffset(0), scaleOffset(1), scaleOffset(4))
        }
    }

    private fun progressionNotesForHandMode(
        chordOffsets: List<Int>,
        handMode: HandMode,
        rightRoot: Int,
        leftRoot: Int
    ): List<Int> = when (handMode) {
        HandMode.LEFT -> chordOffsets.map { leftRoot + it }.sorted()
        HandMode.RIGHT -> chordOffsets.map { rightRoot + it }.sorted()
        HandMode.BOTH -> distributedChordNotesForBothHands(chordOffsets, leftRoot, rightRoot)
    }

    private fun distributedChordNotesForBothHands(
        chordOffsets: List<Int>,
        leftRoot: Int,
        rightRoot: Int
    ): List<Int> {
        val sorted = chordOffsets.sorted()
        if (sorted.isEmpty()) return emptyList()
        if (sorted.size == 1) return listOf(leftRoot + sorted.first(), rightRoot + sorted.first() + 12)

        val leftCount = (sorted.size + 1) / 2
        val leftOffsets = sorted.take(leftCount)
        val rightOffsets = sorted.drop(leftCount)
        val leftNotes = leftOffsets.map { leftRoot + it }
        val rightNotes = if (rightOffsets.isNotEmpty()) {
            rightOffsets.map { rightRoot + it }
        } else {
            listOf(rightRoot + sorted.last())
        }
        return (leftNotes + rightNotes).sorted()
    }

    private fun arpeggiateProgressionChord(notes: List<Int>, random: Random): List<List<Int>> {
        val sortedNotes = notes.sorted()
        if (sortedNotes.size <= 1) return listOf(sortedNotes)

        val contour = ARPEGGIO_CONTOURS.random(random)
        val noteIndices = contour.filter { it < sortedNotes.size }
        val effectiveIndices = if (noteIndices.isNotEmpty()) noteIndices else sortedNotes.indices.toList()
        return effectiveIndices.map { noteIndex -> listOf(sortedNotes[noteIndex]) }
    }

    /** Builds shuffled material for all non-progression types (existing logic). */
    private fun buildShuffledMaterial(
        types: Set<ExerciseContentType>,
        settings: AppSettings,
        generatedKey: Int,
        rightRoot: Int,
        leftRoot: Int,
        random: Random
    ): List<ExerciseStep> {
        if (types.isEmpty()) return emptyList()

        val materialByType = types.associateWith { type ->
            patternsForType(type, settings.handMode, rightRoot, leftRoot, random)
        }.filterValues { it.isNotEmpty() }

        val effectiveMaterialByType = if (materialByType.isEmpty()) {
            mapOf(
                ExerciseContentType.SINGLE_NOTES to patternsForType(
                    ExerciseContentType.SINGLE_NOTES, settings.handMode, rightRoot, leftRoot, random
                )
            )
        } else materialByType

        val seededMix = effectiveMaterialByType.entries
            .shuffled(random)
            .map { entry -> entry.value.random(random) }

        val material = seededMix + effectiveMaterialByType.values.flatten().shuffled(random)

        return if (!settings.noteAccidentalsEnabled) {
            material.map { step ->
                val cn = step.notes.map { constrainNoteToScale(it, generatedKey) }
                step.copy(notes = cn, noteAccidentals = List(cn.size) { NoteAccidental.NONE })
            }
        } else material
    }

    private fun patternsForType(
        type: ExerciseContentType,
        handMode: HandMode,
        rightRoot: Int,
        leftRoot: Int,
        random: Random
    ): List<ExerciseStep> = when (type) {
        ExerciseContentType.SINGLE_NOTES -> sequenceForHandMode(
            handMode,
            SINGLE_NOTE_MOTION.map { ExerciseStep(notes = listOf(leftRoot + it)) },
            SINGLE_NOTE_MOTION.map { ExerciseStep(notes = listOf(rightRoot + it)) },
            random
        )
        ExerciseContentType.OCTAVES -> when (handMode) {
            HandMode.RIGHT -> SCALE_7.map { i -> ExerciseStep(notes = listOf(rightRoot + i, rightRoot + i + 12)) }
            HandMode.LEFT -> SCALE_7.map { i -> ExerciseStep(notes = listOf(leftRoot + i - 12, leftRoot + i)) }
            HandMode.BOTH -> SCALE_7.map { i -> ExerciseStep(notes = listOf(leftRoot + i, rightRoot + i)) }
        }
        ExerciseContentType.THIRDS -> sequenceForHandMode(
            handMode,
            THIRDS.map { (a, b) -> ExerciseStep(notes = listOf(leftRoot + a, leftRoot + b)) },
            THIRDS.map { (a, b) -> ExerciseStep(notes = listOf(rightRoot + a, rightRoot + b)) },
            random
        )
        ExerciseContentType.FIFTHS -> sequenceForHandMode(
            handMode,
            FIFTHS.map { (a, b) -> ExerciseStep(notes = listOf(leftRoot + a, leftRoot + b)) },
            FIFTHS.map { (a, b) -> ExerciseStep(notes = listOf(rightRoot + a, rightRoot + b)) },
            random
        )
        ExerciseContentType.SIXTHS -> sequenceForHandMode(
            handMode,
            SIXTHS.map { (a, b) -> ExerciseStep(notes = listOf(leftRoot + a, leftRoot + b)) },
            SIXTHS.map { (a, b) -> ExerciseStep(notes = listOf(rightRoot + a, rightRoot + b)) },
            random
        )
        ExerciseContentType.ARPEGGIOS -> sequenceForHandMode(
            handMode,
            arpeggioPatterns(leftRoot),
            arpeggioPatterns(rightRoot),
            random
        )
        ExerciseContentType.TRIADS -> sequenceForHandMode(
            handMode,
            TRIADS.map { chord -> ExerciseStep(notes = chord.map { leftRoot + it }) },
            TRIADS.map { chord -> ExerciseStep(notes = chord.map { rightRoot + it }) },
            random
        )
        ExerciseContentType.SEVENTHS -> sequenceForHandMode(
            handMode,
            SEVENTHS.map { chord -> ExerciseStep(notes = chord.map { leftRoot + it }) },
            SEVENTHS.map { chord -> ExerciseStep(notes = chord.map { rightRoot + it }) },
            random
        )
        ExerciseContentType.NINTHS -> sequenceForHandMode(
            handMode,
            NINTHS.map { chord -> ExerciseStep(notes = chord.map { leftRoot + it }) },
            NINTHS.map { chord -> ExerciseStep(notes = chord.map { rightRoot + it }) },
            random
        )
        ExerciseContentType.CLUSTERED_CHORDS -> sequenceForHandMode(
            handMode,
            CLUSTERED_CHORDS.map { chord -> ExerciseStep(notes = chord.map { leftRoot + it }) },
            CLUSTERED_CHORDS.map { chord -> ExerciseStep(notes = chord.map { rightRoot + it }) },
            random
        )
        // PROGRESSIONS are handled separately in buildProgressionSteps(); never reaches here.
        ExerciseContentType.PROGRESSIONS -> emptyList()
    }.map { it.copy(contentType = type) }

    private fun sequenceForHandMode(
        handMode: HandMode,
        leftPattern: List<ExerciseStep>,
        rightPattern: List<ExerciseStep>,
        random: Random
    ): List<ExerciseStep> = when (handMode) {
        HandMode.LEFT -> leftPattern
        HandMode.RIGHT -> rightPattern
        HandMode.BOTH -> (leftPattern + rightPattern).shuffled(random)
    }

    private fun arpeggioPatterns(root: Int): List<ExerciseStep> {
        val chordPool = TRIADS + SEVENTHS + NINTHS + CLUSTERED_CHORDS
        return chordPool.flatMapIndexed { index, chord ->
            val notes = chord.map { root + it }
            val contour = ARPEGGIO_CONTOURS[index % ARPEGGIO_CONTOURS.size]
            contour
                .filter { it < notes.size }
                .map { ExerciseStep(notes = listOf(notes[it])) }
        }
    }

    private fun applyGeneratedAccidentals(
        steps: List<ExerciseStep>,
        enabled: Boolean
    ): List<ExerciseStep> {
        if (!enabled) return steps

        val mutable = steps.map { it.copy() }.toMutableList()
        var index = 1
        while (index < mutable.lastIndex) {
            val current = mutable[index]
            if (current.notes.size != 1 || current.noteAccidentals.any { it != NoteAccidental.NONE }) {
                index++
                continue
            }

            val naturalMidi = naturalMidiFor(current.notes.first())
            val variant = accidentalVariantForNatural(naturalMidi)
            if (variant != null && mutable[index + 1].notes.size == 1) {
                mutable[index] = current.copy(
                    notes = listOf(variant.first),
                    noteAccidentals = listOf(variant.second)
                )
                mutable[index + 1] = mutable[index + 1].copy(
                    notes = listOf(naturalMidi),
                    noteAccidentals = listOf(NoteAccidental.NATURAL)
                )
                index += 4
            } else {
                index++
            }
        }
        return mutable
    }

    /**
     * Applies pedal press/release marks to the step list.
     *
     * Alignment rules:
     * - Pedal press is only placed at a step whose cumulative beat position is
     *   on an integer quarter-note boundary (e.g. 0, 1, 2, 3, 4, …).
     * - Every ~4–8 beats a new press/release pair is injected.
     * - Release snaps to the start of the first beat-boundary step that follows
     *   the press by at least 2 quarter-note beats, within a 6-beat window.
     */
    private fun applyPedalMarks(
        steps: List<ExerciseStep>,
        enabled: Boolean
    ): List<ExerciseStep> {
        if (!enabled || steps.size < 2) return steps

        // Build cumulative beat positions for all steps.
        val beatPositions = run {
            var cursor = 0f
            steps.map { step ->
                val pos = cursor
                cursor += step.noteValue.beats
                pos
            }
        }

        val mutable = steps.map { it.copy() }.toMutableList()
        var index = 0
        var nextEligibleBeat = 0f   // earliest beat at which a new PRESS may be placed

        while (index < mutable.lastIndex) {
            val beat = beatPositions[index]

            // Only place a press at beat-boundary positions (integer quarter-note beats).
            val isOnBeatBoundary = (beat - beat.toLong()) < 0.01f
            val canPress = mutable[index].pedalAction == PedalAction.NONE &&
                           isOnBeatBoundary &&
                           beat >= nextEligibleBeat

            if (!canPress) {
                index++
                continue
            }

            // Find a release: first beat-boundary step ≥ 2 beats after the press, within 6 beats.
            val releaseIndex = (index + 1..mutable.lastIndex).firstOrNull { ri ->
                val rBeat = beatPositions[ri]
                val rIsOnBoundary = (rBeat - rBeat.toLong()) < 0.01f
                rIsOnBoundary &&
                        (rBeat - beat) >= 2f &&
                        (rBeat - beat) <= 6f &&
                        mutable[ri].pedalAction == PedalAction.NONE
            }

            if (releaseIndex != null) {
                mutable[index] = mutable[index].copy(pedalAction = PedalAction.PRESS)
                mutable[releaseIndex] = mutable[releaseIndex].copy(pedalAction = PedalAction.RELEASE)
                nextEligibleBeat = beatPositions[releaseIndex] + 4f  // ≥ 4 beats gap before next press
                index = releaseIndex + 1
            } else {
                index++
            }
        }

        return mutable
    }

    /**
     * Clamps each note in every step to the configured bass/treble MIDI ranges.
     * Notes are transposed by octaves (±12) until they fall within the configured range.
     * If a note cannot be placed within range after octave shifting, it is snapped to the
     * nearest boundary note in the same pitch class.
     *
     * Split point: MIDI < 60 → bass staff range; MIDI ≥ 60 → treble staff range.
     */
    private fun applyNoteRanges(
        steps: List<ExerciseStep>,
        settings: AppSettings,
        key: Int
    ): List<ExerciseStep> {
        val bassMin = settings.bassNoteRangeMin.coerceIn(28, 72)
        val bassMax = settings.bassNoteRangeMax.coerceIn(bassMin, 72)
        val trebleMin = settings.trebleNoteRangeMin.coerceIn(48, 93)
        val trebleMax = settings.trebleNoteRangeMax.coerceIn(trebleMin, 93)

        return steps.map { step ->
            val clampedNotes = step.notes.map { midi ->
                clampMidiToRange(midi, if (midi < 60) bassMin to bassMax else trebleMin to trebleMax)
            }
            if (clampedNotes == step.notes) step
            else step.copy(
                notes = clampedNotes,
                noteAccidentals = if (settings.noteAccidentalsEnabled) {
                    // Recompute accidentals are reset to NONE after clamping
                    List(clampedNotes.size) { NoteAccidental.NONE }
                } else step.noteAccidentals
            )
        }
    }

    /**
     * Randomly assigns ornaments (TRILL, MORDENT, TURN) to approximately 1 in 6 steps
     * when [enabled]. Only quarter-note or longer steps receive ornaments (grace notes
     * on eighth notes look crowded). The main note is never changed — ornaments are
     * decorative only and do not affect note matching.
     */
    private fun applyOrnaments(
        steps: List<ExerciseStep>,
        selectedOrnaments: Set<OrnamentType>,
        random: Random
    ): List<ExerciseStep> {
        if (selectedOrnaments.isEmpty()) return steps
        return steps.map { step ->
            if (step.ornament == OrnamentType.NONE &&
                step.noteValue.beats >= 1f &&
                step.notes.isNotEmpty() &&
                random.nextInt(6) == 0
            ) {
                // Arpeggiation requires a chord; all other ornaments require a single note.
                val eligible = selectedOrnaments.filter { type ->
                    if (type == OrnamentType.ARPEGGIATION) step.notes.size > 1
                    else step.notes.size == 1
                }
                if (eligible.isEmpty()) step else step.copy(ornament = eligible.random(random))
            } else step
        }
    }

    private fun clampMidiToRange(midi: Int, range: Pair<Int, Int>): Int {
        val (min, max) = range
        if (midi in min..max) return midi
        // Shift by octaves
        var shifted = midi
        while (shifted < min) shifted += 12
        while (shifted > max) shifted -= 12
        if (shifted in min..max) return shifted
        // Snap to nearest boundary
        return if (kotlin.math.abs(shifted - min) <= kotlin.math.abs(shifted - max)) min else max
    }

    private fun naturalMidiFor(midi: Int): Int = when (midi % 12) {
        1, 3, 6, 8, 10 -> midi - 1
        else -> midi
    }

    private fun accidentalVariantForNatural(naturalMidi: Int): Pair<Int, NoteAccidental>? = when (naturalMidi % 12) {
        0, 5 -> (naturalMidi + 1) to NoteAccidental.SHARP
        2, 7, 9 -> if ((naturalMidi / 12) % 2 == 0) {
            (naturalMidi + 1) to NoteAccidental.SHARP
        } else {
            (naturalMidi - 1) to NoteAccidental.FLAT
        }
        4, 11 -> (naturalMidi - 1) to NoteAccidental.FLAT
        else -> null
    }

    private fun constrainNoteToScale(note: Int, key: Int): Int {
        val scalePitchClasses = SCALE_7.map { (it + key) % 12 }.toSet()
        if ((note % 12 + 12) % 12 in scalePitchClasses) return note

        val quickCandidates = listOf(1, -1, 2, -2)
        quickCandidates.forEach { delta ->
            val candidate = note + delta
            if (((candidate % 12) + 12) % 12 in scalePitchClasses) {
                return candidate
            }
        }

        for (delta in 3..6) {
            val up = note + delta
            if (((up % 12) + 12) % 12 in scalePitchClasses) return up
            val down = note - delta
            if (((down % 12) + 12) % 12 in scalePitchClasses) return down
        }
        return note
    }
}
