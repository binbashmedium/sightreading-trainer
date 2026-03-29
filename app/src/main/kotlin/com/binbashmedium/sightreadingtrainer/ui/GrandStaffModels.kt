package com.binbashmedium.sightreadingtrainer.ui

import kotlin.math.absoluteValue

enum class NoteState {
    NONE,
    CORRECT,
    WRONG,
    LATE
}

data class NoteEvent(
    val midi: Int,
    val startBeat: Float,
    val duration: Float,
    val expected: Boolean,
    val state: NoteState = NoteState.NONE
)

data class Chord(
    val name: String,
    val notes: List<Int>,
    val startBeat: Float
)

data class GameState(
    val levelTitle: String,
    val elapsedTime: Long,
    val score: Int,
    val notes: List<NoteEvent>,
    val chords: List<Chord>,
    val currentBeat: Float
)

fun formatElapsedTime(elapsedTimeMs: Long): String {
    val totalSeconds = (elapsedTimeMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

fun midiToGrandStaffY(
    midi: Int,
    trebleTopY: Float,
    bassTopY: Float,
    lineSpacing: Float
): Float {
    return if (midi >= 60) {
        val semitoneOffset = 77 - midi
        trebleTopY + (semitoneOffset * (lineSpacing / 2f))
    } else {
        val semitoneOffset = 57 - midi
        bassTopY + (semitoneOffset * (lineSpacing / 2f))
    }
}

fun beatToX(
    beat: Float,
    startX: Float,
    beatWidth: Float
): Float = startX + (beat * beatWidth)

fun durationToGlyphType(duration: Float): NoteGlyphType {
    val normalized = duration.absoluteValue
    return when {
        normalized >= 4f -> NoteGlyphType.WHOLE
        normalized >= 2f -> NoteGlyphType.HALF
        normalized >= 1f -> NoteGlyphType.QUARTER
        normalized >= 0.5f -> NoteGlyphType.EIGHTH
        else -> NoteGlyphType.SIXTEENTH
    }
}

enum class NoteGlyphType {
    WHOLE,
    HALF,
    QUARTER,
    EIGHTH,
    SIXTEENTH
}
