package com.binbashmedium.sightreadingtrainer.domain.model

enum class NoteAccidental {
    NONE,
    SHARP,
    FLAT,
    NATURAL
}

enum class PedalAction {
    NONE,
    PRESS,
    RELEASE
}

data class ExerciseStep(
    val notes: List<Int> = emptyList(),
    val noteAccidentals: List<NoteAccidental> = List(notes.size) { NoteAccidental.NONE },
    val pedalAction: PedalAction = PedalAction.NONE
) {
    init {
        require(noteAccidentals.size == notes.size) {
            "noteAccidentals size must match notes size"
        }
    }
}

data class PerformanceInput(
    val notes: List<NoteEvent> = emptyList(),
    val pedalAction: PedalAction = PedalAction.NONE,
    val timestamp: Long = notes.firstOrNull()?.timestamp ?: System.currentTimeMillis()
)

data class PedalEvent(
    val action: PedalAction,
    val timestamp: Long = System.currentTimeMillis()
)
