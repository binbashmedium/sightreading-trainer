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

        val chords = when (settings.difficulty) {
            1    -> generateLevel1(settings.handMode, rightRoot, leftRoot)
            2    -> generateLevel2(rightRoot, leftRoot)
            3    -> generateLevel3(rightRoot)
            4    -> generateLevel4(rightRoot)
            5    -> generateLevel5(rightRoot)
            else -> generateLevel1(settings.handMode, rightRoot, leftRoot)
        }
        return Exercise(chords, musicalKey = K)
    }

    /** Level 1: single notes from the diatonic scale (one hand), shuffled each time. */
    private fun generateLevel1(handMode: HandMode, rightRoot: Int, leftRoot: Int): List<List<Int>> {
        val root = if (handMode == HandMode.LEFT) leftRoot else rightRoot
        return SCALE_8.map { listOf(root + it) }.shuffled()
    }

    /** Level 2: parallel octaves (both hands), shuffled each time. */
    private fun generateLevel2(rightRoot: Int, leftRoot: Int): List<List<Int>> =
        SCALE_7.map { i -> listOf(leftRoot + i, rightRoot + i) }.shuffled()

    /** Level 3: diatonic thirds in the right hand, shuffled each time. */
    private fun generateLevel3(root: Int): List<List<Int>> =
        THIRDS.map { (a, b) -> listOf(root + a, root + b) }.shuffled()

    /** Level 4: all 7 diatonic triads in root position, shuffled each time. */
    private fun generateLevel4(root: Int): List<List<Int>> =
        TRIADS.map { (a, b, c) -> listOf(root + a, root + b, root + c) }.shuffled()

    /** Level 5: randomly pick one of several common 4-chord progressions. */
    private fun generateLevel5(root: Int): List<List<Int>> {
        val prog = PROGRESSIONS[Random.nextInt(PROGRESSIONS.size)]
        return prog.map { triadIdx ->
            val (a, b, c) = TRIADS[triadIdx]
            listOf(root + a, root + b, root + c)
        }
    }
}
