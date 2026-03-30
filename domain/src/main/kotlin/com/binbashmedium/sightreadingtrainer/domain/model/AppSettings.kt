package com.binbashmedium.sightreadingtrainer.domain.model

data class AppSettings(
    val midiDeviceName: String = "",
    val timingToleranceMs: Int = 200,
    val chordWindowMs: Int = 50,
    val exerciseTimeSec: Int = 60,
    val exerciseLength: Int = 8,
    val exerciseTypes: Set<ExerciseContentType> = setOf(ExerciseContentType.SINGLE_NOTES),
    val handMode: HandMode = HandMode.RIGHT,
    val noteAccidentalsEnabled: Boolean = false,
    val pedalEventsEnabled: Boolean = false,
    val highScore: Int = 0,
    val totalCorrectNotes: Int = 0,
    val totalWrongNotes: Int = 0,
    val soundEnabled: Boolean = true,
    /** Selectable pool of keys (0 = C, 1 = C#/Db, 2 = D, …, 11 = B). */
    val selectedKeys: Set<Int> = setOf(0)
)

enum class HandMode { LEFT, RIGHT, BOTH }

enum class ExerciseContentType {
    SINGLE_NOTES,
    OCTAVES,
    THIRDS,
    FIFTHS,
    SIXTHS,
    ARPEGGIOS,
    TRIADS,
    SEVENTHS,
    NINTHS,
    CLUSTERED_CHORDS
}
