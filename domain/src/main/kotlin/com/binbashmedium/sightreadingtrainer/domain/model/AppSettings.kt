package com.binbashmedium.sightreadingtrainer.domain.model

data class AppSettings(
    val midiDeviceName: String = "",
    val timingToleranceMs: Int = 200,
    val chordWindowMs: Int = 50,
    val difficulty: Int = 1,
    val handMode: HandMode = HandMode.RIGHT,
    val soundEnabled: Boolean = true
)

enum class HandMode { LEFT, RIGHT, BOTH }
