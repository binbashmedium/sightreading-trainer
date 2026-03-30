package com.binbashmedium.sightreadingtrainer.domain.model

data class AppSettings(
    val midiDeviceName: String = "",
    val timingToleranceMs: Int = 200,
    val chordWindowMs: Int = 50,
    val difficulty: Int = 1,
    val exerciseLength: Int = 8,
    val handMode: HandMode = HandMode.RIGHT,
    val soundEnabled: Boolean = true,
    /** 0 = C, 1 = C#/Db, 2 = D, …, 11 = B */
    val musicalKey: Int = 0
)

enum class HandMode { LEFT, RIGHT, BOTH }
