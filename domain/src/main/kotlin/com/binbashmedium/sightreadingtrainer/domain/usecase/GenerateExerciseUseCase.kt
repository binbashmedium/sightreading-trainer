package com.binbashmedium.sightreadingtrainer.domain.usecase

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode

class GenerateExerciseUseCase {

    // C major scale: C4=60, D4=62, E4=64, F4=65, G4=67, A4=69, B4=71, C5=72
    private val cMajorScaleRight = listOf(60, 62, 64, 65, 67, 69, 71, 72)

    // C major scale for the left hand — all notes strictly below MIDI 60 (< C4)
    private val cMajorScaleLeft = listOf(48, 50, 52, 53, 55, 57, 59) // C3–B3

    fun execute(settings: AppSettings): Exercise {
        val chords = when (settings.difficulty) {
            1 -> generateLevel1(settings.handMode)
            2 -> generateLevel2()
            3 -> generateLevel3()
            4 -> generateLevel4()
            5 -> generateLevel5()
            else -> generateLevel1(settings.handMode)
        }
        return Exercise(chords)
    }

    /** Level 1: single notes up the C major scale, one hand. */
    private fun generateLevel1(handMode: HandMode): List<List<Int>> {
        val scale = when (handMode) {
            HandMode.LEFT -> cMajorScaleLeft
            HandMode.RIGHT -> cMajorScaleRight
            HandMode.BOTH -> cMajorScaleRight
        }
        return scale.map { listOf(it) }
    }

    /** Level 2: both hands in parallel octaves. */
    private fun generateLevel2(): List<List<Int>> =
        cMajorScaleRight.zip(cMajorScaleLeft).map { (r, l) -> listOf(l, r) }

    /** Level 3: thirds in the right hand. */
    private fun generateLevel3(): List<List<Int>> = listOf(
        listOf(60, 64), // C+E
        listOf(62, 65), // D+F
        listOf(64, 67), // E+G
        listOf(65, 69), // F+A
        listOf(67, 71), // G+B
        listOf(69, 72), // A+C5
        listOf(71, 74)  // B+D5
    )

    /** Level 4: major and minor triads in root position. */
    private fun generateLevel4(): List<List<Int>> = listOf(
        listOf(60, 64, 67), // C major
        listOf(62, 65, 69), // D minor
        listOf(64, 67, 71), // E minor
        listOf(65, 69, 72), // F major
        listOf(67, 71, 74), // G major
        listOf(69, 72, 76), // A minor
        listOf(60, 64, 67)  // C major (repeat)
    )

    /** Level 5: I–IV–V–I chord progression in C major. */
    private fun generateLevel5(): List<List<Int>> = listOf(
        listOf(60, 64, 67), // I:  C major
        listOf(65, 69, 72), // IV: F major
        listOf(67, 71, 74), // V:  G major
        listOf(60, 64, 67)  // I:  C major
    )
}
