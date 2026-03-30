package com.binbashmedium.sightreadingtrainer.domain.usecase

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseStep
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteAccidental
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction

class GenerateExerciseUseCase {

    companion object {
        val KEY_NAMES = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

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

    fun execute(settings: AppSettings, forcedKey: Int? = null): Exercise {
        val generatedKey = (forcedKey ?: settings.selectedKeys.ifEmpty { setOf(0) }.toList().random()).coerceIn(0, 11)
        val rightRoot = 60 + generatedKey
        val leftRoot = 48 + generatedKey
        val maxDisplayedNotes = settings.exerciseLength.coerceAtLeast(1)
        val selectedTypes = settings.exerciseTypes.ifEmpty { setOf(ExerciseContentType.SINGLE_NOTES) }

        val materialByType = selectedTypes.associateWith { type ->
            patternsForType(type, settings.handMode, rightRoot, leftRoot)
        }.filterValues { it.isNotEmpty() }

        val effectiveMaterialByType = if (materialByType.isEmpty()) {
            mapOf(
                ExerciseContentType.SINGLE_NOTES to patternsForType(
                    ExerciseContentType.SINGLE_NOTES,
                    settings.handMode,
                    rightRoot,
                    leftRoot
                )
            )
        } else {
            materialByType
        }

        val seededMix = effectiveMaterialByType.entries
            .shuffled()
            .map { entry -> entry.value.random() }

        val material = seededMix + effectiveMaterialByType.values.flatten().shuffled()

        val accidentalPolicyMaterial = if (settings.noteAccidentalsEnabled) {
            material
        } else {
            material.map { step ->
                val constrainedNotes = step.notes.map { constrainNoteToScale(it, generatedKey) }
                step.copy(
                    notes = constrainedNotes,
                    noteAccidentals = List(constrainedNotes.size) { NoteAccidental.NONE }
                )
            }
        }

        val mixedExercise = stepsByMaxNotes(accidentalPolicyMaterial.shuffled(), maxDisplayedNotes)

        val accidentalsApplied = applyGeneratedAccidentals(mixedExercise, settings.noteAccidentalsEnabled)
        val pedalApplied = applyPedalMarks(accidentalsApplied, settings.pedalEventsEnabled)

        return Exercise(steps = pedalApplied, musicalKey = generatedKey, handMode = settings.handMode)
    }

    private fun patternsForType(
        type: ExerciseContentType,
        handMode: HandMode,
        rightRoot: Int,
        leftRoot: Int
    ): List<ExerciseStep> = when (type) {
        ExerciseContentType.SINGLE_NOTES -> sequenceForHandMode(
            handMode,
            SINGLE_NOTE_MOTION.map { ExerciseStep(notes = listOf(leftRoot + it)) },
            SINGLE_NOTE_MOTION.map { ExerciseStep(notes = listOf(rightRoot + it)) }
        )
        ExerciseContentType.OCTAVES -> when (handMode) {
            HandMode.RIGHT -> SCALE_7.map { i -> ExerciseStep(notes = listOf(rightRoot + i, rightRoot + i + 12)) }
            HandMode.LEFT -> SCALE_7.map { i -> ExerciseStep(notes = listOf(leftRoot + i - 12, leftRoot + i)) }
            HandMode.BOTH -> SCALE_7.map { i -> ExerciseStep(notes = listOf(leftRoot + i, rightRoot + i)) }
        }
        ExerciseContentType.THIRDS -> sequenceForHandMode(
            handMode,
            THIRDS.map { (a, b) -> ExerciseStep(notes = listOf(leftRoot + a, leftRoot + b)) },
            THIRDS.map { (a, b) -> ExerciseStep(notes = listOf(rightRoot + a, rightRoot + b)) }
        )
        ExerciseContentType.FIFTHS -> sequenceForHandMode(
            handMode,
            FIFTHS.map { (a, b) -> ExerciseStep(notes = listOf(leftRoot + a, leftRoot + b)) },
            FIFTHS.map { (a, b) -> ExerciseStep(notes = listOf(rightRoot + a, rightRoot + b)) }
        )
        ExerciseContentType.SIXTHS -> sequenceForHandMode(
            handMode,
            SIXTHS.map { (a, b) -> ExerciseStep(notes = listOf(leftRoot + a, leftRoot + b)) },
            SIXTHS.map { (a, b) -> ExerciseStep(notes = listOf(rightRoot + a, rightRoot + b)) }
        )
        ExerciseContentType.ARPEGGIOS -> sequenceForHandMode(
            handMode,
            arpeggioPatterns(leftRoot),
            arpeggioPatterns(rightRoot)
        )
        ExerciseContentType.TRIADS -> sequenceForHandMode(
            handMode,
            TRIADS.map { chord -> ExerciseStep(notes = chord.map { leftRoot + it }) },
            TRIADS.map { chord -> ExerciseStep(notes = chord.map { rightRoot + it }) }
        )
        ExerciseContentType.SEVENTHS -> sequenceForHandMode(
            handMode,
            SEVENTHS.map { chord -> ExerciseStep(notes = chord.map { leftRoot + it }) },
            SEVENTHS.map { chord -> ExerciseStep(notes = chord.map { rightRoot + it }) }
        )
        ExerciseContentType.NINTHS -> sequenceForHandMode(
            handMode,
            NINTHS.map { chord -> ExerciseStep(notes = chord.map { leftRoot + it }) },
            NINTHS.map { chord -> ExerciseStep(notes = chord.map { rightRoot + it }) }
        )
        ExerciseContentType.CLUSTERED_CHORDS -> sequenceForHandMode(
            handMode,
            CLUSTERED_CHORDS.map { chord -> ExerciseStep(notes = chord.map { leftRoot + it }) },
            CLUSTERED_CHORDS.map { chord -> ExerciseStep(notes = chord.map { rightRoot + it }) }
        )
    }.map { it.copy(contentType = type) }

    private fun sequenceForHandMode(
        handMode: HandMode,
        leftPattern: List<ExerciseStep>,
        rightPattern: List<ExerciseStep>
    ): List<ExerciseStep> = when (handMode) {
        HandMode.LEFT -> leftPattern
        HandMode.RIGHT -> rightPattern
        HandMode.BOTH -> (leftPattern + rightPattern).shuffled()
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

    private fun applyPedalMarks(
        steps: List<ExerciseStep>,
        enabled: Boolean
    ): List<ExerciseStep> {
        if (!enabled || steps.size < 2) return steps

        val mutable = steps.map { it.copy() }.toMutableList()
        var index = 0
        while (index < mutable.lastIndex) {
            val shouldAddPedal = mutable[index].pedalAction == PedalAction.NONE && index % 5 == 0
            if (!shouldAddPedal) {
                index++
                continue
            }

            val releaseIndex = (index + 1..minOf(index + 3, mutable.lastIndex))
                .firstOrNull { mutable[it].pedalAction == PedalAction.NONE }

            if (releaseIndex != null) {
                mutable[index] = mutable[index].copy(pedalAction = PedalAction.PRESS)
                mutable[releaseIndex] = mutable[releaseIndex].copy(pedalAction = PedalAction.RELEASE)
                index = releaseIndex + 1
            } else {
                index++
            }
        }

        return mutable
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

    private fun stepsByMaxNotes(base: List<ExerciseStep>, maxNotes: Int): List<ExerciseStep> {
        if (base.isEmpty()) return emptyList()

        val result = mutableListOf<ExerciseStep>()
        var noteBudget = 0
        var index = 0
        while (noteBudget < maxNotes) {
            val step = base[index % base.size]
            val notesCount = step.notes.size
            val candidate = if (notesCount > maxNotes) {
                step.copy(
                    notes = step.notes.take(maxNotes),
                    noteAccidentals = step.noteAccidentals.take(maxNotes)
                )
            } else {
                step
            }
            val candidateCount = candidate.notes.size
            if (result.isNotEmpty() && noteBudget + candidateCount > maxNotes) break
            result += candidate
            noteBudget += candidateCount
            if (candidateCount == 0 && result.size > maxNotes) break
            index++
        }
        return if (result.isEmpty()) listOf(base.first()) else result
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
