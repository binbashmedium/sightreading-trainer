package com.binbashmedium.sightreadingtrainer.domain.model

/**
 * Represents a practice exercise consisting of a sequence of chords to play.
 * Each chord is a list of MIDI note numbers.
 */
data class Exercise(
    val expectedNotes: List<List<Int>>,
    val currentIndex: Int = 0
) {
    val isComplete: Boolean
        get() = currentIndex >= expectedNotes.size

    val currentChord: List<Int>?
        get() = expectedNotes.getOrNull(currentIndex)
}
