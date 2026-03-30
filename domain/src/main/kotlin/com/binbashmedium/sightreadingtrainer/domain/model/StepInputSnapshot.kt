package com.binbashmedium.sightreadingtrainer.domain.model

data class StepInputSnapshot(
    val playedNotes: List<Int> = emptyList(),
    val playedPedalAction: PedalAction = PedalAction.NONE,
    val inputTimestampMs: Long = 0L,
    val pedalPressedAtInput: Boolean = false,
    val lastPedalReleaseTimestampMs: Long? = null
)
