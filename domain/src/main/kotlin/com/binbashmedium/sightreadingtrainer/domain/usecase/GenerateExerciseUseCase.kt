package com.binbashmedium.sightreadingtrainer.domain.usecase

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import kotlin.random.Random

class GenerateExerciseUseCase {

    companion object {
        val KEY_NAMES = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

        /** Diatonic major scale semitone offsets from root (7 notes). */
        private val SCALE_7 = listOf(0, 2, 4, 5, 7, 9, 11)

        /** Diatonic major scale semitone offsets including the octave (8 notes). */
        private val SCALE_8 = listOf(0, 2, 4, 5, 7, 9, 11, 12)

        /** Diatonic thirds (two notes a diatonic third apart), as pairs of semitone offsets from root. */
        private val THIRDS = listOf(
            0 to 4, 2 to 5, 4 to 7, 5 to 9, 7 to 11, 9 to 12, 11 to 14
        )

        /** Diatonic triads (root position) as semitone offset triples from root. */
        private val TRIADS = listOf(
            Triple(0, 4, 7),   // I   major
            Triple(2, 5, 9),   // ii  minor
            Triple(4, 7, 11),  // iii minor
            Triple(5, 9, 12),  // IV  major
            Triple(7, 11, 14), // V   major
            Triple(9, 12, 16), // vi  minor
            Triple(11, 14, 17) // vii diminished
        )

        /** Common 4-chord progressions as lists of TRIADS indices. */
        private val PROGRESSIONS = listOf(
            listOf(0, 3, 4, 0),  // I  – IV – V  – I
            listOf(0, 4, 5, 3),  // I  – V  – vi – IV
            listOf(0, 5, 3, 4),  // I  – vi – IV – V
            listOf(0, 1, 4, 0)   // I  – ii – V  – I
        )
    }

    fun execute(settings: AppSettings): Exercise {
        val K = settings.musicalKey
        val rightRoot = 60 + K   // C4 + key offset (treble)
        val leftRoot  = 48 + K   // C3 + key offset (bass)
        val exerciseLength = settings.exerciseLength.coerceAtLeast(1)

        val chords = when (settings.difficulty) {
            1    -> generateLevel1(settings.handMode, rightRoot, leftRoot, exerciseLength)
            2    -> generateLevel2(settings.handMode, rightRoot, leftRoot, exerciseLength)
            3    -> generateLevel3(settings.handMode, rightRoot, leftRoot, exerciseLength)
            4    -> generateLevel4(settings.handMode, rightRoot, leftRoot, exerciseLength)
            5    -> generateLevel5(settings.handMode, rightRoot, leftRoot, exerciseLength)
            else -> generateLevel1(settings.handMode, rightRoot, leftRoot, exerciseLength)
        }
        return Exercise(chords, musicalKey = K, handMode = settings.handMode)
    }

    /** Level 1: single notes from the diatonic scale (one hand), shuffled each time. */
    private fun generateLevel1(
        handMode: HandMode,
        rightRoot: Int,
        leftRoot: Int,
        length: Int
    ): List<List<Int>> {
        val rightPattern = SCALE_8.map { listOf(rightRoot + it) }
        val leftPattern = SCALE_8.map { listOf(leftRoot + it) }
        return sequenceForHandMode(handMode, leftPattern, rightPattern, length)
    }

    /** Level 2: octaves in one hand, or split octaves across both hands. */
    private fun generateLevel2(
        handMode: HandMode,
        rightRoot: Int,
        leftRoot: Int,
        length: Int
    ): List<List<Int>> {
        val rightPattern = SCALE_7.map { i -> listOf(rightRoot + i, rightRoot + i + 12) }
        val leftPattern = SCALE_7.map { i -> listOf(leftRoot + i - 12, leftRoot + i) }
        return when (handMode) {
            HandMode.RIGHT -> repeatPattern(rightPattern, length)
            HandMode.LEFT -> repeatPattern(leftPattern, length)
            HandMode.BOTH -> repeatPattern(SCALE_7.map { i -> listOf(leftRoot + i, rightRoot + i) }, length)
        }
    }

    /** Level 3: diatonic thirds in the selected hand(s). */
    private fun generateLevel3(
        handMode: HandMode,
        rightRoot: Int,
        leftRoot: Int,
        length: Int
    ): List<List<Int>> {
        val rightPattern = THIRDS.map { (a, b) -> listOf(rightRoot + a, rightRoot + b) }
        val leftPattern = THIRDS.map { (a, b) -> listOf(leftRoot + a, leftRoot + b) }
        return sequenceForHandMode(handMode, leftPattern, rightPattern, length)
    }

    /** Level 4: diatonic triads in the selected hand(s). */
    private fun generateLevel4(
        handMode: HandMode,
        rightRoot: Int,
        leftRoot: Int,
        length: Int
    ): List<List<Int>> {
        val rightPattern = TRIADS.map { (a, b, c) -> listOf(rightRoot + a, rightRoot + b, rightRoot + c) }
        val leftPattern = TRIADS.map { (a, b, c) -> listOf(leftRoot + a, leftRoot + b, leftRoot + c) }
        return sequenceForHandMode(handMode, leftPattern, rightPattern, length)
    }

    /** Level 5: common progressions in the selected hand(s). */
    private fun generateLevel5(
        handMode: HandMode,
        rightRoot: Int,
        leftRoot: Int,
        length: Int
    ): List<List<Int>> {
        val rightPattern = progressionPattern(rightRoot)
        val leftPattern = progressionPattern(leftRoot)
        return sequenceForHandMode(handMode, leftPattern, rightPattern, length)
    }

    private fun progressionPattern(root: Int): List<List<Int>> {
        val progression = PROGRESSIONS[Random.nextInt(PROGRESSIONS.size)]
        return progression.map { triadIdx ->
            val (a, b, c) = TRIADS[triadIdx]
            listOf(root + a, root + b, root + c)
        }
    }

    private fun sequenceForHandMode(
        handMode: HandMode,
        leftPattern: List<List<Int>>,
        rightPattern: List<List<Int>>,
        length: Int
    ): List<List<Int>> = when (handMode) {
        HandMode.LEFT -> repeatPattern(leftPattern, length)
        HandMode.RIGHT -> repeatPattern(rightPattern, length)
        HandMode.BOTH -> alternateHands(leftPattern, rightPattern, length)
    }

    private fun repeatPattern(pattern: List<List<Int>>, length: Int): List<List<Int>> {
        if (pattern.isEmpty()) return emptyList()
        val result = mutableListOf<List<Int>>()
        while (result.size < length) {
            result += pattern.shuffled().take(length - result.size)
        }
        return result
    }

    private fun alternateHands(
        leftPattern: List<List<Int>>,
        rightPattern: List<List<Int>>,
        length: Int
    ): List<List<Int>> {
        val leftNotes = repeatPattern(leftPattern, (length + 1) / 2)
        val rightNotes = repeatPattern(rightPattern, length / 2)
        return buildList(length) {
            var leftIndex = 0
            var rightIndex = 0
            repeat(length) { index ->
                add(
                    if (index % 2 == 0) {
                        leftNotes[leftIndex++]
                    } else {
                        rightNotes[rightIndex++]
                    }
                )
            }
        }
    }
}
