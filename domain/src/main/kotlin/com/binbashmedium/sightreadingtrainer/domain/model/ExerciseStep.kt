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

/**
 * Musical ornament type for a note.
 * When applied, the ornament symbol is displayed above (or before) the note.
 * The main note pitch is still the only one evaluated during practice.
 */
enum class OrnamentType {
    NONE,
    /** Rapid alternation between main note and the note a step above. */
    TRILL,
    /** Lower mordent — quick alternation downward: main → lower → main. */
    MORDENT,
    /** Inverted (upper) mordent — quick alternation upward: main → upper → main. */
    UPPER_MORDENT,
    /** Turn — four-note ornament circling the main note: upper → main → lower → main. */
    TURN,
    /** Acciaccatura — small crushed note (semitone below) played instantly before the main note. */
    GRACE_NOTE
}

data class ExerciseStep(
    val notes: List<Int> = emptyList(),
    val noteAccidentals: List<NoteAccidental> = List(notes.size) { NoteAccidental.NONE },
    val pedalAction: PedalAction = PedalAction.NONE,
    val contentType: ExerciseContentType? = null,
    val noteValue: NoteValue = NoteValue.QUARTER,
    /** Ornament applied to the first note of this step. Does not affect note matching. */
    val ornament: OrnamentType = OrnamentType.NONE
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
