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
    fun `clef baselines are aligned from their correct anchor lines`() {
        assertEquals(172.4f, trebleClefBaselineY(160f, 20f))
        assertEquals(373.6f, bassClefBaselineY(320f, 20f))
    }

    @Test
    fun `bass clef dots are centered around the F line and top reaches A line`() {
        val geometry = bassClefGeometry(
            clefX = 20f,
            trebleTopY = 100f,
            bassTopY = 300f,
            lineSpacing = 20f
        )
        val fLineY = staffLineYForStep(BASS_F_LINE_STEP, StaffType.BASS, 100f, 300f, 20f)
        val aLineY = staffLineYForStep(BASS_TOP_LINE_STEP, StaffType.BASS, 100f, 300f, 20f)

        assertEquals(310f, geometry.upperDotY)
        assertEquals(330f, geometry.lowerDotY)
        assertEquals(fLineY, (geometry.upperDotY + geometry.lowerDotY) / 2f)
        assertEquals(aLineY, geometry.topY)
    }

    @Test
    fun `key signature proportions stay smaller than the clefs`() {
        val metrics = grandStaffLayoutMetrics(lineSpacing = 20f, numAccidentals = 4)

        assertEquals(65f, metrics.clefAreaWidth, 0.001f)
        assertEquals(43f, metrics.postClefGap, 0.001f)
        assertEquals(8.4f, metrics.accidentalSpacing, 0.001f)
        assertEquals(19.2f, metrics.accidentalTextSize, 0.001f)
        assertEquals(92f, metrics.trebleClefTextSize, 0.001f)
        assertTrue(metrics.accidentalTextSize < metrics.trebleClefTextSize / 4f)
        assertTrue(metrics.keySignatureWidth < metrics.clefAreaWidth)
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
    fun `formatChordLabel names triads sevenths and ninths with roman numerals`() {
        assertEquals("C4", formatChordLabel(listOf(60), 0))
        assertEquals("C4 - E4", formatChordLabel(listOf(60, 64), 0))
        assertEquals("CM (I)", formatChordLabel(listOf(60, 64, 67), 0))
        assertEquals("CM (I)", formatChordLabel(listOf(64, 67, 72), 0))
        assertEquals("CM7 (Imaj7)", formatChordLabel(listOf(60, 64, 67, 71), 0))
        assertEquals("CM7 (Imaj7)", formatChordLabel(listOf(64, 67, 71, 72), 0))
        assertEquals("CM9 (Imaj9)", formatChordLabel(listOf(60, 64, 67, 71, 74), 0))
        assertEquals("G9 (V9)", formatChordLabel(listOf(67, 71, 74, 77, 81), 0))
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
