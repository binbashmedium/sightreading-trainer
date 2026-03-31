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

data class PracticeState(
    val exercise: Exercise,
    val sessionDurationSec: Int = 60,
    val lastResult: MatchResult = MatchResult.Waiting,
    val score: Int = 0,
    val totalAttempts: Int = 0,
    val correctNotesCount: Int = 0,
    val wrongNotesCount: Int = 0,
    val startTimeMs: Long = System.currentTimeMillis(),
    /** Per-chord match results keyed by chord index, for accurate per-beat colouring. */
    val resultByBeat: Map<Int, MatchResult> = emptyMap(),
    /** Played input snapshot for each resolved beat index (notes + pedal context). */
    val inputByBeat: Map<Int, StepInputSnapshot> = emptyMap(),
    /** Session-local performance counters by generated exercise group (for example TRIADS). */
    val correctGroupStats: Map<String, Int> = emptyMap(),
    val wrongGroupStats: Map<String, Int> = emptyMap(),
    /** Session-local per-note performance counters (for example C4, F#3). */
    val correctNoteStats: Map<String, Int> = emptyMap(),
    val wrongNoteStats: Map<String, Int> = emptyMap(),
    /** Live BPM from the last inter-chord interval (0 until 2nd correct chord). */
    val bpm: Float = 0f,
    /** Timestamp of the most-recent correct chord (for BPM + fluency scoring). */
    val lastCorrectTimestamp: Long = 0L
)
