package com.binbashmedium.sightreadingtrainer.domain.model

/**
 * Represents a practice exercise consisting of a sequence of chords to play.
 * Each chord is a list of MIDI note numbers.
 */
data class Exercise(
    val steps: List<ExerciseStep>,
    val currentIndex: Int = 0,
    /** Key used when this exercise was generated (0=C … 11=B). */
    val musicalKey: Int = 0,
    /** Hand mode used when this exercise was generated. */
    val handMode: HandMode = HandMode.RIGHT
) {
    val expectedNotes: List<List<Int>>
        get() = steps.map { it.notes }

    val currentStep: ExerciseStep?
        get() = steps.getOrNull(currentIndex)

    val isComplete: Boolean
        get() = currentIndex >= steps.size

    val currentChord: List<Int>?
        get() = currentStep?.notes
}
