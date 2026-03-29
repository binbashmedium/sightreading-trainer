package com.binbashmedium.sightreadingtrainer.domain.model

data class NoteEvent(
    val midiNote: Int,
    val velocity: Int,
    val timestamp: Long = System.currentTimeMillis()
)
