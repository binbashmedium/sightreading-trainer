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

package com.binbashmedium.sightreadingtrainer

import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseStep
import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import com.binbashmedium.sightreadingtrainer.domain.usecase.MatchNotesUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchNotesUseCaseTest {

    private val useCase = MatchNotesUseCase()

    @Test
    fun `correct single note returns Correct`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(60, 100)),
            playedPedalAction = PedalAction.NONE,
            expectedStep = ExerciseStep(notes = listOf(60))
        )
        assertEquals(MatchResult.Correct, result)
    }

    @Test
    fun `wrong single note returns Incorrect`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(62, 100)),
            playedPedalAction = PedalAction.NONE,
            expectedStep = ExerciseStep(notes = listOf(60))
        )
        assertEquals(MatchResult.Incorrect, result)
    }

    @Test
    fun `correct chord returns Correct`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(60, 100), NoteEvent(64, 100), NoteEvent(67, 100)),
            playedPedalAction = PedalAction.NONE,
            expectedStep = ExerciseStep(notes = listOf(60, 64, 67))
        )
        assertEquals(MatchResult.Correct, result)
    }

    @Test
    fun `chord played in different order still matches`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(67, 100), NoteEvent(60, 100), NoteEvent(64, 100)),
            playedPedalAction = PedalAction.NONE,
            expectedStep = ExerciseStep(notes = listOf(60, 64, 67))
        )
        assertEquals(MatchResult.Correct, result)
    }

    @Test
    fun `extra note played returns Incorrect`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(60, 100), NoteEvent(64, 100), NoteEvent(67, 100), NoteEvent(72, 100)),
            playedPedalAction = PedalAction.NONE,
            expectedStep = ExerciseStep(notes = listOf(60, 64, 67))
        )
        assertEquals(MatchResult.Incorrect, result)
    }

    @Test
    fun `too early note returns TooEarly`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(60, 100, timestamp = 700L)),
            playedPedalAction = PedalAction.NONE,
            expectedStep = ExerciseStep(notes = listOf(60)),
            toleranceMs = 200L,
            expectedTimeMs = 1000L
        )
        assertEquals(MatchResult.TooEarly, result)
    }

    @Test
    fun `too late note returns TooLate`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(60, 100, timestamp = 1300L)),
            playedPedalAction = PedalAction.NONE,
            expectedStep = ExerciseStep(notes = listOf(60)),
            toleranceMs = 200L,
            expectedTimeMs = 1000L
        )
        assertEquals(MatchResult.TooLate, result)
    }

    @Test
    fun `note within tolerance returns Correct`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(60, 100, timestamp = 1100L)),
            playedPedalAction = PedalAction.NONE,
            expectedStep = ExerciseStep(notes = listOf(60)),
            toleranceMs = 200L,
            expectedTimeMs = 1000L
        )
        assertEquals(MatchResult.Correct, result)
    }

    @Test
    fun `pedal mismatch returns Incorrect`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(60, 100)),
            playedPedalAction = PedalAction.PRESS,
            expectedStep = ExerciseStep(notes = listOf(60), pedalAction = PedalAction.RELEASE)
        )
        assertEquals(MatchResult.Incorrect, result)
    }

    @Test
    fun `incidental pedal input does not fail note-only step`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(60, 100)),
            playedPedalAction = PedalAction.PRESS,
            expectedStep = ExerciseStep(notes = listOf(60), pedalAction = PedalAction.NONE)
        )
        assertEquals(MatchResult.Correct, result)
    }

    @Test
    fun `expected press accepts pedal already pressed before notes`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(60, 100, timestamp = 2_000L)),
            playedPedalAction = PedalAction.NONE,
            expectedStep = ExerciseStep(notes = listOf(60), pedalAction = PedalAction.PRESS),
            pedalIsPressed = true
        )
        assertEquals(MatchResult.Correct, result)
    }

    @Test
    fun `expected release accepts recent release shortly before notes`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(60, 100, timestamp = 2_500L)),
            playedPedalAction = PedalAction.NONE,
            expectedStep = ExerciseStep(notes = listOf(60), pedalAction = PedalAction.RELEASE),
            pedalIsPressed = false,
            lastPedalReleaseTimestampMs = 2_000L,
            releaseLeadToleranceMs = 1_000L
        )
        assertEquals(MatchResult.Correct, result)
    }

    @Test
    fun `expected release fails when release was too early`() {
        val result = useCase.execute(
            playedNotes = listOf(NoteEvent(60, 100, timestamp = 3_500L)),
            playedPedalAction = PedalAction.NONE,
            expectedStep = ExerciseStep(notes = listOf(60), pedalAction = PedalAction.RELEASE),
            pedalIsPressed = false,
            lastPedalReleaseTimestampMs = 2_000L,
            releaseLeadToleranceMs = 1_000L
        )
        assertEquals(MatchResult.Incorrect, result)
    }
}
