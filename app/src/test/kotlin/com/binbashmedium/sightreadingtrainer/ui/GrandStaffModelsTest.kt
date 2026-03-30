package com.binbashmedium.sightreadingtrainer.ui

import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrandStaffModelsTest {

    @Test
    fun `formatElapsedTime returns mm ss`() {
        assertEquals("00:00", formatElapsedTime(0L))
        assertEquals("01:05", formatElapsedTime(65_000L))
        assertEquals("10:00", formatElapsedTime(600_000L))
    }

    @Test
    fun `staff anchor lines map to correct y positions`() {
        val trebleY = staffLineYForStep(
            diatonicStep = TREBLE_G_LINE_STEP,
            staff = StaffType.TREBLE,
            trebleTopY = 100f,
            bassTopY = 300f,
            lineSpacing = 20f
        )
        val bassY = staffLineYForStep(
            diatonicStep = BASS_F_LINE_STEP,
            staff = StaffType.BASS,
            trebleTopY = 100f,
            bassTopY = 300f,
            lineSpacing = 20f
        )

        assertEquals(160f, trebleY)
        assertEquals(320f, bassY)
    }

    @Test
    fun `midiToGrandStaffY routes midi 60 and above to treble`() {
        val trebleY = midiToGrandStaffY(
            midi = 60,
            staff = StaffType.TREBLE,
            trebleTopY = 100f,
            bassTopY = 300f,
            lineSpacing = 20f
        )
        val bassY = midiToGrandStaffY(
            midi = 59,
            staff = StaffType.BASS,
            trebleTopY = 100f,
            bassTopY = 300f,
            lineSpacing = 20f
        )

        assertTrue(trebleY < bassY)
    }

    @Test
    fun `staffForExercise honors explicit hand mode overrides`() {
        assertEquals(StaffType.TREBLE, staffForExercise(48, HandMode.RIGHT))
        assertEquals(StaffType.BASS, staffForExercise(72, HandMode.LEFT))
        assertEquals(StaffType.BASS, staffForExercise(59, HandMode.BOTH))
        assertEquals(StaffType.TREBLE, staffForExercise(60, HandMode.BOTH))
    }

    @Test
    fun `durationToGlyphType maps supported durations`() {
        assertEquals(NoteGlyphType.WHOLE, durationToGlyphType(4f))
        assertEquals(NoteGlyphType.HALF, durationToGlyphType(2f))
        assertEquals(NoteGlyphType.QUARTER, durationToGlyphType(1f))
        assertEquals(NoteGlyphType.EIGHTH, durationToGlyphType(0.5f))
        assertEquals(NoteGlyphType.SIXTEENTH, durationToGlyphType(0.25f))
    }

    @Test
    fun `generateExampleGameState produces dynamic content`() {
        val now = 1_700_000_000_000L
        val state = generateExampleGameState(now)

        assertTrue(state.levelTitle.isNotEmpty())
        assertTrue(state.notes.isNotEmpty())
        assertTrue(state.chords.isNotEmpty())
        assertTrue(state.score > 0)
    }

    @Test
    fun `generateExampleGameState bpm field is non-negative`() {
        val state = generateExampleGameState(System.currentTimeMillis())
        assertTrue(state.bpm >= 0f)
    }
}
