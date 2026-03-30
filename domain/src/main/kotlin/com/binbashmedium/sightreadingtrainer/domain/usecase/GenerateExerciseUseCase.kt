package com.binbashmedium.sightreadingtrainer.domain.usecase

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode

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
    }

    fun execute(settings: AppSettings): Exercise {
        val generatedKey = settings.selectedKeys.ifEmpty { setOf(0) }.toList().random()
        val rightRoot = 60 + generatedKey
        val leftRoot = 48 + generatedKey
        val exerciseLength = settings.exerciseLength.coerceAtLeast(1)
        val selectedTypes = settings.exerciseTypes.ifEmpty { setOf(ExerciseContentType.SINGLE_NOTES) }

        val material = selectedTypes.flatMap { type ->
            patternsForType(type, settings.handMode, rightRoot, leftRoot)
        }.ifEmpty {
            patternsForType(ExerciseContentType.SINGLE_NOTES, settings.handMode, rightRoot, leftRoot)
        }

        val mixedExercise = material.shuffled().let { base ->
            buildList(exerciseLength) {
                var index = 0
                repeat(exerciseLength) {
                    add(base[index % base.size])
                    index++
                }
            }
        }

        return Exercise(mixedExercise.shuffled(), musicalKey = generatedKey, handMode = settings.handMode)
    }

    private fun patternsForType(
        type: ExerciseContentType,
        handMode: HandMode,
        rightRoot: Int,
        leftRoot: Int
        ): List<List<Int>> = when (type) {
        ExerciseContentType.SINGLE_NOTES -> sequenceForHandMode(
            handMode,
            SINGLE_NOTE_MOTION.map { listOf(leftRoot + it) },
            SINGLE_NOTE_MOTION.map { listOf(rightRoot + it) }
        )
        ExerciseContentType.OCTAVES -> when (handMode) {
            HandMode.RIGHT -> SCALE_7.map { i -> listOf(rightRoot + i, rightRoot + i + 12) }
            HandMode.LEFT -> SCALE_7.map { i -> listOf(leftRoot + i - 12, leftRoot + i) }
            HandMode.BOTH -> SCALE_7.map { i -> listOf(leftRoot + i, rightRoot + i) }
        }
        ExerciseContentType.THIRDS -> sequenceForHandMode(
            handMode,
            THIRDS.map { (a, b) -> listOf(leftRoot + a, leftRoot + b) },
            THIRDS.map { (a, b) -> listOf(rightRoot + a, rightRoot + b) }
        )
        ExerciseContentType.FIFTHS -> sequenceForHandMode(
            handMode,
            FIFTHS.map { (a, b) -> listOf(leftRoot + a, leftRoot + b) },
            FIFTHS.map { (a, b) -> listOf(rightRoot + a, rightRoot + b) }
        )
        ExerciseContentType.SIXTHS -> sequenceForHandMode(
            handMode,
            SIXTHS.map { (a, b) -> listOf(leftRoot + a, leftRoot + b) },
            SIXTHS.map { (a, b) -> listOf(rightRoot + a, rightRoot + b) }
        )
        ExerciseContentType.TRIADS -> sequenceForHandMode(
            handMode,
            TRIADS.map { chord -> chord.map { leftRoot + it } },
            TRIADS.map { chord -> chord.map { rightRoot + it } }
        )
        ExerciseContentType.SEVENTHS -> sequenceForHandMode(
            handMode,
            SEVENTHS.map { chord -> chord.map { leftRoot + it } },
            SEVENTHS.map { chord -> chord.map { rightRoot + it } }
        )
        ExerciseContentType.NINTHS -> sequenceForHandMode(
            handMode,
            NINTHS.map { chord -> chord.map { leftRoot + it } },
            NINTHS.map { chord -> chord.map { rightRoot + it } }
        )
        ExerciseContentType.CLUSTERED_CHORDS -> sequenceForHandMode(
            handMode,
            CLUSTERED_CHORDS.map { chord -> chord.map { leftRoot + it } },
            CLUSTERED_CHORDS.map { chord -> chord.map { rightRoot + it } }
        )
    }

    private fun sequenceForHandMode(
        handMode: HandMode,
        leftPattern: List<List<Int>>,
        rightPattern: List<List<Int>>
    ): List<List<Int>> = when (handMode) {
        HandMode.LEFT -> leftPattern
        HandMode.RIGHT -> rightPattern
        HandMode.BOTH -> (leftPattern + rightPattern).shuffled()
    }
}
