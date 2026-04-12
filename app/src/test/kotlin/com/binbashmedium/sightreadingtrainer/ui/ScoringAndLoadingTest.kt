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

package com.binbashmedium.sightreadingtrainer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringAndLoadingTest {

    // ── computeHighScore ─────────────────────────────────────────────────────

    @Test
    fun `computeHighScore returns 0 when no notes played`() {
        assertEquals(0, computeHighScore(0, 0, 60f, 60L))
    }

    @Test
    fun `computeHighScore increases with better accuracy`() {
        val lowAccuracy  = computeHighScore(correctNotes = 5,  wrongNotes = 5,  bpm = 120f, practiceTimeSec = 60L)
        val highAccuracy = computeHighScore(correctNotes = 10, wrongNotes = 0,  bpm = 120f, practiceTimeSec = 60L)
        assertTrue("Better accuracy should yield higher score", highAccuracy > lowAccuracy)
    }

    @Test
    fun `computeHighScore increases with higher BPM`() {
        val slowScore = computeHighScore(correctNotes = 10, wrongNotes = 0, bpm = 60f,  practiceTimeSec = 60L)
        val fastScore = computeHighScore(correctNotes = 10, wrongNotes = 0, bpm = 120f, practiceTimeSec = 60L)
        assertTrue("Higher BPM should yield higher score", fastScore > slowScore)
    }

    @Test
    fun `computeHighScore increases with longer practice time`() {
        val shortSession = computeHighScore(correctNotes = 10, wrongNotes = 0, bpm = 120f, practiceTimeSec = 30L)
        val longSession  = computeHighScore(correctNotes = 10, wrongNotes = 0, bpm = 120f, practiceTimeSec = 120L)
        assertTrue("Longer practice should yield higher score", longSession > shortSession)
    }

    @Test
    fun `computeHighScore clamps BPM above 240`() {
        val cappedBpm   = computeHighScore(correctNotes = 10, wrongNotes = 0, bpm = 240f,  practiceTimeSec = 60L)
        val exceedBpm   = computeHighScore(correctNotes = 10, wrongNotes = 0, bpm = 9999f, practiceTimeSec = 60L)
        assertEquals("BPM > 240 should be treated same as 240", cappedBpm, exceedBpm)
    }

    @Test
    fun `computeHighScore is non-negative for all-wrong scenario`() {
        val score = computeHighScore(correctNotes = 0, wrongNotes = 20, bpm = 0f, practiceTimeSec = 60L)
        assertTrue("Score should be non-negative", score >= 0)
    }

    // ── SessionResultUi.accuracy ──────────────────────────────────────────────

    @Test
    fun `SessionResultUi accuracy is 100 percent when all correct`() {
        val result = SessionResultUi(
            correctNotes = 10, wrongNotes = 0,
            bpm = 120f, practiceTimeSec = 60L,
            highScore = 0, isNewHighScore = false
        )
        assertEquals(100, result.accuracy)
    }

    @Test
    fun `SessionResultUi accuracy is 0 percent when all wrong`() {
        val result = SessionResultUi(
            correctNotes = 0, wrongNotes = 10,
            bpm = 120f, practiceTimeSec = 60L,
            highScore = 0, isNewHighScore = false
        )
        assertEquals(0, result.accuracy)
    }

    @Test
    fun `SessionResultUi accuracy is 50 percent for half correct`() {
        val result = SessionResultUi(
            correctNotes = 5, wrongNotes = 5,
            bpm = 120f, practiceTimeSec = 60L,
            highScore = 0, isNewHighScore = false
        )
        assertEquals(50, result.accuracy)
    }

    @Test
    fun `SessionResultUi accuracy is 0 when no notes played`() {
        val result = SessionResultUi(
            correctNotes = 0, wrongNotes = 0,
            bpm = 0f, practiceTimeSec = 0L,
            highScore = 0, isNewHighScore = false
        )
        assertEquals(0, result.accuracy)
    }

    // ── GameState no longer has score field ──────────────────────────────────

    @Test
    fun `shouldStopLoadingOnRender returns true only when state exists and loading is active`() {
        assertTrue(shouldStopLoadingOnRender(hasState = true, isLoading = true))
        assertEquals(false, shouldStopLoadingOnRender(hasState = false, isLoading = true))
        assertEquals(false, shouldStopLoadingOnRender(hasState = true, isLoading = false))
    }

    @Test
    fun `generateExampleGameState has no score field and has expected fields`() {
        val state = generateExampleGameState(1_700_000_000_000L)
        assertTrue(state.levelTitle.isNotEmpty())
        assertTrue(state.notes.isNotEmpty())
        assertTrue(state.bpm >= 0f)
        assertTrue(state.elapsedTime >= 0L)
    }

    @Test
    fun `generateExampleGameState bpm field changes over time`() {
        // At phase=0 (nowMs=0), bpm should be 0
        val earlyState = generateExampleGameState(nowMs = 0L)
        assertEquals(0f, earlyState.bpm, 0f)

        // At a later phase where phase > 1 (nowMs = 2000+), bpm should be > 0
        val laterState = generateExampleGameState(nowMs = 3_000L) // phase = 3
        assertTrue("BPM should be positive at later phases", laterState.bpm > 0f)
    }
}
