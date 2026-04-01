// Copyright 2026 BinBashMedium
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.binbashmedium.sightreadingtrainer.domain.model

data class AppSettings(
    val midiDeviceName: String = "",
    val timingToleranceMs: Int = 200,
    val chordWindowMs: Int = 50,
    val exerciseTimeSec: Int = 60,
    val exerciseTypes: Set<ExerciseContentType> = setOf(ExerciseContentType.SINGLE_NOTES),
    val handMode: HandMode = HandMode.RIGHT,
    val noteAccidentalsEnabled: Boolean = false,
    val pedalEventsEnabled: Boolean = false,
    val highScore: Int = 0,
    val totalCorrectNotes: Int = 0,
    val totalWrongNotes: Int = 0,
    val correctGroupStats: Map<String, Int> = emptyMap(),
    val wrongGroupStats: Map<String, Int> = emptyMap(),
    val correctNoteStats: Map<String, Int> = emptyMap(),
    val wrongNoteStats: Map<String, Int> = emptyMap(),
    val soundEnabled: Boolean = true,
    /** Selectable pool of keys (0 = C, 1 = C#/Db, 2 = D, …, 11 = B). */
    val selectedKeys: Set<Int> = setOf(0),
    /** Active chord progressions used when [ExerciseContentType.PROGRESSIONS] is selected. */
    val selectedProgressions: Set<ChordProgression> = setOf(ChordProgression.I_IV_V_I)
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
    CLUSTERED_CHORDS,
    PROGRESSIONS
}

/**
 * Named diatonic chord progressions built on the major scale.
 * Each [chords] entry is a list of scale-degree offset lists (semitones from the key root).
 * All chords are diatonic triads; the offsets follow the pattern [root, third, fifth].
 */
enum class ChordProgression(
    val displayName: String,
    val chords: List<List<Int>>
) {
    /** I – IV – V – I  (classical perfect cadence) */
    I_IV_V_I(
        "I-IV-V-I",
        listOf(
            listOf(0, 4, 7),   // I
            listOf(5, 9, 12),  // IV
            listOf(7, 11, 14), // V
            listOf(0, 4, 7)    // I
        )
    ),
    /** I – V – vi – IV  (pop progression) */
    I_V_VI_IV(
        "I-V-vi-IV",
        listOf(
            listOf(0, 4, 7),   // I
            listOf(7, 11, 14), // V
            listOf(9, 12, 16), // vi
            listOf(5, 9, 12)   // IV
        )
    ),
    /** ii – V – I  (jazz turnaround) */
    II_V_I(
        "ii-V-I",
        listOf(
            listOf(2, 5, 9),   // ii
            listOf(7, 11, 14), // V
            listOf(0, 4, 7)    // I
        )
    ),
    /** I – vi – IV – V  (50s progression) */
    I_VI_IV_V(
        "I-vi-IV-V",
        listOf(
            listOf(0, 4, 7),   // I
            listOf(9, 12, 16), // vi
            listOf(5, 9, 12),  // IV
            listOf(7, 11, 14)  // V
        )
    ),
    /** I – IV – I – V  (blues turnaround) */
    I_IV_I_V(
        "I-IV-I-V",
        listOf(
            listOf(0, 4, 7),   // I
            listOf(5, 9, 12),  // IV
            listOf(0, 4, 7),   // I
            listOf(7, 11, 14)  // V
        )
    ),
    /** vi – IV – I – V  (minor-feel pop) */
    VI_IV_I_V(
        "vi-IV-I-V",
        listOf(
            listOf(9, 12, 16), // vi
            listOf(5, 9, 12),  // IV
            listOf(0, 4, 7),   // I
            listOf(7, 11, 14)  // V
        )
    ),
    /** I – iii – IV – V  (ascending walk) */
    I_III_IV_V(
        "I-iii-IV-V",
        listOf(
            listOf(0, 4, 7),   // I
            listOf(4, 7, 11),  // iii
            listOf(5, 9, 12),  // IV
            listOf(7, 11, 14)  // V
        )
    )
}
