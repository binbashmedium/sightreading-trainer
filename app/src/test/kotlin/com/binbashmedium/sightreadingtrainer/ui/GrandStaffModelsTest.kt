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
    fun `bass clef baseline is placed sufficiently below A line so glyph renders below it`() {
        val geometry = bassClefGeometry(
            clefX = 20f,
            trebleTopY = 100f,
            bassTopY = 300f,
            lineSpacing = 20f
        )
        // Baseline must be >= 0.88 * textSize below topY so the glyph ascent clears the A line
        val minBaseline = geometry.topY + geometry.textSize * 0.88f
        assertTrue(
            "Baseline ${geometry.baselineY} should be >= $minBaseline",
            geometry.baselineY >= minBaseline
        )
    }

    @Test
    fun `key signature accidentals are proportionally larger than before`() {
        val metrics = grandStaffLayoutMetrics(lineSpacing = 20f, numAccidentals = 4)

        assertEquals(65f, metrics.clefAreaWidth, 0.001f)
        assertEquals(43f, metrics.postClefGap, 0.001f)
        assertEquals(15f, metrics.accidentalSpacing, 0.001f)       // 20 * 0.75
        assertEquals(44f, metrics.accidentalTextSize, 0.001f)      // 20 * 2.2
        assertEquals(92f, metrics.trebleClefTextSize, 0.001f)
        // Accidentals must be clearly visible (> 1 lineSpacing) but smaller than the clef
        assertTrue(metrics.accidentalTextSize > metrics.trebleClefTextSize / 4f)
        assertTrue(metrics.accidentalTextSize < metrics.trebleClefTextSize)
        assertTrue(metrics.keySignatureWidth < metrics.clefAreaWidth)
    }

    @Test
    fun `key signature accidental order uses standard staff positions`() {
        assertEquals(38, TREBLE_SHARP_STEPS.first())
        assertEquals(24, BASS_SHARP_STEPS.first())
        assertEquals(34, TREBLE_FLAT_STEPS.first())
        assertEquals(20, BASS_FLAT_STEPS.first())
        assertEquals(listOf(38, 35, 39, 36), TREBLE_SHARP_STEPS.take(4))
        assertEquals(listOf(34, 37, 33, 36), TREBLE_FLAT_STEPS.take(4))
    }

    @Test
    fun `ledger line helpers return correct staff lines`() {
        assertEquals(listOf(28, 26), ledgerStepsBelow(26, TREBLE_BOTTOM_LINE_STEP))
        assertEquals(listOf(40, 42), ledgerStepsAbove(42, TREBLE_TOP_LINE_STEP))
        assertEquals(listOf(16, 14), ledgerStepsBelow(14, BASS_BOTTOM_LINE_STEP))
        assertEquals(listOf(28, 30), ledgerStepsAbove(30, BASS_TOP_LINE_STEP))
    }

    @Test
    fun `stem direction follows middle line engraving rules`() {
        assertEquals(StemDirection.UP, stemDirectionForSteps(listOf(30), StaffType.TREBLE))
        assertEquals(StemDirection.DOWN, stemDirectionForSteps(listOf(TREBLE_MIDDLE_LINE_STEP), StaffType.TREBLE))
        assertEquals(StemDirection.DOWN, stemDirectionForSteps(listOf(32, 34, 38), StaffType.TREBLE))
        assertEquals(StemDirection.UP, stemDirectionForSteps(listOf(18, 20, 23), StaffType.BASS))
        assertEquals(StemDirection.DOWN, stemDirectionForSteps(listOf(20, 24), StaffType.BASS))
    }

    @Test
    fun `seconds in chords displace noteheads by one notehead width`() {
        // Displacement must equal lineSpacing * 1.1 so displaced note clears the stem
        val lineSpacing = 20f
        val expectedDisplacement = lineSpacing * 1.1f  // = 22f

        assertEquals(
            listOf(0f, expectedDisplacement),
            chordNoteheadOffsets(listOf(32, 33), StemDirection.UP, lineSpacing)
        )
        assertEquals(
            listOf(-expectedDisplacement, 0f),
            chordNoteheadOffsets(listOf(32, 33), StemDirection.DOWN, lineSpacing)
        )
    }

    @Test
    fun `cluster noteheads that are adjacent seconds displace away from stem`() {
        // Three-note cluster: lower two are a second apart
        val lineSpacing = 20f
        val d = lineSpacing * 1.1f  // expected displacement

        // UP stem: middle note displaced right (it's adjacent to note below it)
        val upOffsets = chordNoteheadOffsets(listOf(32, 33, 35), StemDirection.UP, lineSpacing)
        assertEquals(0f, upOffsets[0])
        assertEquals(d, upOffsets[1])
        assertEquals(0f, upOffsets[2])

        // DOWN stem: lower of the adjacent pair displaced left
        val downOffsets = chordNoteheadOffsets(listOf(32, 33, 35), StemDirection.DOWN, lineSpacing)
        assertEquals(-d, downOffsets[0])
        assertEquals(0f, downOffsets[1])
        assertEquals(0f, downOffsets[2])
    }

    @Test
    fun `stem x for cluster uses non-displaced notehead position`() {
        val noteHeadWidth = 22f
        val lineSpacing = 20f

        // UP stem: displaced note is at larger x; stem must anchor to leftmost (non-displaced) x
        val upStem = stemGeometryForChord(
            noteXs = listOf(100f, 122f),  // non-displaced at 100, displaced at 122
            noteYs = listOf(210f, 200f),
            direction = StemDirection.UP,
            middleLineY = 180f,
            lineSpacing = lineSpacing,
            noteHeadWidth = noteHeadWidth
        )
        assertEquals(100f + noteHeadWidth * 0.5f, upStem.x, 0.001f)  // 111f

        // DOWN stem: displaced note is at smaller x; stem must anchor to rightmost (non-displaced) x
        val downStem = stemGeometryForChord(
            noteXs = listOf(100f, 78f),   // non-displaced at 100, displaced at 78
            noteYs = listOf(120f, 130f),
            direction = StemDirection.DOWN,
            middleLineY = 140f,
            lineSpacing = lineSpacing,
            noteHeadWidth = noteHeadWidth
        )
        assertEquals(100f - noteHeadWidth * 0.5f, downStem.x, 0.001f)  // 89f
    }

    @Test
    fun `stem geometry reaches middle line when notes are far from it`() {
        val upStem = stemGeometryForChord(
            noteXs = listOf(100f, 122f),
            noteYs = listOf(210f, 200f),
            direction = StemDirection.UP,
            middleLineY = 180f,
            lineSpacing = 20f,
            noteHeadWidth = 22f
        )
        val downStem = stemGeometryForChord(
            noteXs = listOf(100f, 78f),
            noteYs = listOf(120f, 130f),
            direction = StemDirection.DOWN,
            middleLineY = 140f,
            lineSpacing = 20f,
            noteHeadWidth = 22f
        )

        assertEquals(111f, upStem.x, 0.001f)
        assertEquals(210f, upStem.startY, 0.001f)
        assertEquals(130f, upStem.endY, 0.001f)   // min(200-70, 180) = min(130,180) = 130
        assertEquals(89f, downStem.x, 0.001f)
        assertEquals(120f, downStem.startY, 0.001f)
        assertEquals(200f, downStem.endY, 0.001f)  // max(130+70, 140) = max(200,140) = 200
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
