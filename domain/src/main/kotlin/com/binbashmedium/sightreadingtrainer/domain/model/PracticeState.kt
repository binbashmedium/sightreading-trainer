package com.binbashmedium.sightreadingtrainer.domain.model

data class PracticeState(
    val exercise: Exercise,
    val lastResult: MatchResult = MatchResult.Waiting,
    val score: Int = 0,
    val totalAttempts: Int = 0,
    val startTimeMs: Long = System.currentTimeMillis()
)
