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

/**
 * Represents a practice exercise consisting of a sequence of chords to play.
 * Each chord is a list of MIDI note numbers.
 */
data class Exercise(
    val steps: List<ExerciseStep>,
    val currentIndex: Int = 0,
    /** Key used when this exercise was generated (0=C … 11=B). */
    val musicalKey: Int = 0,
    /** Hand mode used when this exercise was generated. */
    val handMode: HandMode = HandMode.RIGHT
) {
    val expectedNotes: List<List<Int>>
        get() = steps.map { it.notes }

    val currentStep: ExerciseStep?
        get() = steps.getOrNull(currentIndex)

    val isComplete: Boolean
        get() = currentIndex >= steps.size

    val currentChord: List<Int>?
        get() = currentStep?.notes
}
