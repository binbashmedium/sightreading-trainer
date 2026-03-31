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

data class ExerciseStep(
    val notes: List<Int> = emptyList(),
    val noteAccidentals: List<NoteAccidental> = List(notes.size) { NoteAccidental.NONE },
    val pedalAction: PedalAction = PedalAction.NONE,
    val contentType: ExerciseContentType? = null
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
