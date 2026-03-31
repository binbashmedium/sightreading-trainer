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

package com.binbashmedium.sightreadingtrainer.domain.usecase

import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseStep
import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction

class MatchNotesUseCase {

    fun execute(
        playedNotes: List<NoteEvent>,
        playedPedalAction: PedalAction,
        expectedStep: ExerciseStep,
        toleranceMs: Long = 200L,
        expectedTimeMs: Long? = null,
        pedalIsPressed: Boolean = false,
        lastPedalReleaseTimestampMs: Long? = null,
        releaseLeadToleranceMs: Long = 1_000L,
        playedInputTimestampMs: Long? = null
    ): MatchResult {
        val playedMidiNotes = playedNotes.map { it.midiNote }.sorted()
        val sortedExpected = expectedStep.notes.sorted()

        if (playedMidiNotes != sortedExpected) {
            return MatchResult.Incorrect
        }

        when (expectedStep.pedalAction) {
            PedalAction.NONE -> Unit
            PedalAction.PRESS -> {
                val pedalSatisfied = playedPedalAction == PedalAction.PRESS || pedalIsPressed
                if (!pedalSatisfied) return MatchResult.Incorrect
            }
            PedalAction.RELEASE -> {
                val playedTime = playedNotes.firstOrNull()?.timestamp
                    ?: playedInputTimestampMs
                    ?: System.currentTimeMillis()
                val releaseOnThisInput = playedPedalAction == PedalAction.RELEASE
                val releaseWasRecent = lastPedalReleaseTimestampMs != null &&
                    playedTime >= lastPedalReleaseTimestampMs &&
                    playedTime - lastPedalReleaseTimestampMs <= releaseLeadToleranceMs
                if (!releaseOnThisInput && !releaseWasRecent) return MatchResult.Incorrect
            }
        }

        if (expectedTimeMs != null) {
            val playedTime = playedNotes.firstOrNull()?.timestamp ?: return MatchResult.Correct
            val delta = playedTime - expectedTimeMs
            return when {
                delta < -toleranceMs -> MatchResult.TooEarly
                delta > toleranceMs -> MatchResult.TooLate
                else -> MatchResult.Correct
            }
        }

        return MatchResult.Correct
    }
}
