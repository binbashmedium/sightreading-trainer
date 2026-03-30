package com.binbashmedium.sightreadingtrainer.domain.model

data class PracticeState(
    val exercise: Exercise,
    val lastResult: MatchResult = MatchResult.Waiting,
    val score: Int = 0,
    val totalAttempts: Int = 0,
    val startTimeMs: Long = System.currentTimeMillis(),
    /** Per-chord match results keyed by chord index, for accurate per-beat colouring. */
    val resultByBeat: Map<Int, MatchResult> = emptyMap(),
    /** Live BPM from the last inter-chord interval (0 until 2nd correct chord). */
    val bpm: Float = 0f,
    /** Timestamp of the most-recent correct chord (for BPM + fluency scoring). */
    val lastCorrectTimestamp: Long = 0L
)
