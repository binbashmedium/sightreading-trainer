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
 * Rhythmic duration of a note or chord as displayed on the staff.
 *
 * This affects how the note head is drawn (open vs filled) and whether a
 * stem and/or flag is attached.  It does not currently affect the timing
 * tolerance used during note matching, but it could be used in future
 * beat-synchronised exercise modes.
 *
 *   WHOLE   = open head, no stem         (4 beats in 4/4)
 *   HALF    = open head, with stem       (2 beats)
 *   QUARTER = filled head, with stem     (1 beat)
 *   EIGHTH  = filled head, stem + flag   (½ beat)
 */
enum class NoteValue {
    WHOLE,
    HALF,
    QUARTER,
    EIGHTH;

    /** Number of beats this value occupies in 4/4 time. */
    val beats: Float get() = when (this) {
        WHOLE   -> 4f
        HALF    -> 2f
        QUARTER -> 1f
        EIGHTH  -> 0.5f
    }
}
