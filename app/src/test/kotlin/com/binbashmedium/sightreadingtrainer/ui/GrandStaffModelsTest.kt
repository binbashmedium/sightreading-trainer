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

import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseStep
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteAccidental
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import com.binbashmedium.sightreadingtrainer.domain.model.StepInputSnapshot
import org.junit.Assert.assertFalse
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
        assertEquals(179f, trebleClefBaselineY(160f, 20f))
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

        assertEquals(76f, metrics.clefAreaWidth, 0.001f)           // 20 * 3.8
        assertEquals(43f, metrics.postClefGap, 0.001f)
        assertEquals(15f, metrics.accidentalSpacing, 0.001f)       // 20 * 0.75
        assertEquals(44f, metrics.accidentalTextSize, 0.001f)      // 20 * 2.2
        assertEquals(80f, metrics.trebleClefTextSize, 0.001f)
        // Accidentals must be clearly visible (> 1 lineSpacing) but smaller than the clef
        assertTrue(metrics.accidentalTextSize > metrics.trebleClefTextSize / 4f)
        assertTrue(metrics.accidentalTextSize < metrics.trebleClefTextSize)
        // Key sig (4 accidentals) must fit within the (now wider) clef area
        assertTrue(metrics.keySignatureWidth < metrics.clefAreaWidth)
    }

    @Test
    fun `formatChordLabelShort omits roman numeral and stays compact`() {
        // Single note — just the note name
        assertEquals("C4", formatChordLabelShort(listOf(60)))
        // Interval — note-note
        assertEquals("C4-E4", formatChordLabelShort(listOf(60, 64)))
        // Triad — root + quality, no parenthetical
        assertEquals("CM", formatChordLabelShort(listOf(60, 64, 67)))
        assertEquals("Dm", formatChordLabelShort(listOf(62, 65, 69)))
        assertEquals("C5", formatChordLabelShort(listOf(60, 67)))
        assertEquals("Caug", formatChordLabelShort(listOf(60, 64, 68)))
        assertEquals("Cdim7", formatChordLabelShort(listOf(60, 63, 66, 69)))
        assertEquals("C6", formatChordLabelShort(listOf(60, 64, 67, 69)))
        assertEquals("Cm6", formatChordLabelShort(listOf(60, 63, 67, 69)))
        assertEquals("Cadd9", formatChordLabelShort(listOf(60, 64, 67, 74)))
        // Suspended chord voicing (D-G-A) should resolve to Gsus2 instead of note list
        assertEquals("Gsus2", formatChordLabelShort(listOf(62, 67, 69)))
        // Seventh chord
        assertEquals("CM7", formatChordLabelShort(listOf(60, 64, 67, 71)))
        assertEquals("C11", formatChordLabelShort(listOf(60, 64, 67, 70, 74, 77)))
        assertEquals("C13", formatChordLabelShort(listOf(60, 64, 67, 70, 74, 81)))
        assertEquals("C7b9", formatChordLabelShort(listOf(60, 64, 67, 70, 73)))
    }

    @Test
    fun `chord staff is BASS only when all notes are below middle C in BOTH mode`() {
        // All below 60 → bass
        val bassChord = listOf(48, 52, 55)
        val bassStaffs = bassChord.map { staffForExercise(it, HandMode.BOTH) }
        assertTrue(bassStaffs.all { it == StaffType.BASS })

        // Any note >= 60 → treble label
        val trebleChord = listOf(60, 64, 67)
        val trebleStaffs = trebleChord.map { staffForExercise(it, HandMode.BOTH) }
        assertTrue(trebleStaffs.all { it == StaffType.TREBLE })
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
    fun `note accidental symbols expose sharp flat and natural glyphs`() {
        assertEquals("♯", noteAccidentalSymbol(NoteAccidental.SHARP))
        assertEquals("♭", noteAccidentalSymbol(NoteAccidental.FLAT))
        assertEquals("♮", noteAccidentalSymbol(NoteAccidental.NATURAL))
        assertEquals(null, noteAccidentalSymbol(NoteAccidental.NONE))
    }

    @Test
    fun `flat accidentals shift display step to the upper natural note`() {
        assertEquals(midiToDiatonicStep(61) + 1, displayDiatonicStep(61, NoteAccidental.FLAT))
        assertEquals(midiToDiatonicStep(61), displayDiatonicStep(61, NoteAccidental.SHARP))
    }

    @Test
    fun `pedal marks map to the expected text`() {
        assertEquals("Ped.", pedalMarkText(PedalAction.PRESS))
        assertEquals("✱", pedalMarkText(PedalAction.RELEASE))
        assertEquals(null, pedalMarkText(PedalAction.NONE))
    }

    @Test
    fun `close accidentals in chords use additional left columns`() {
        assertEquals(listOf(0, 1, 2), accidentalColumnsForSteps(listOf(32, 33, 34)))
        assertEquals(listOf(0, 0, 1), accidentalColumnsForSteps(listOf(30, 35, 37)))
        assertEquals(emptyList<Int>(), accidentalColumnsForSteps(emptyList()))
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
    fun `BAR_LINE_SHIFT_RATIO provides at least notehead-width gap after bar line`() {
        // Gap from bar line to first notehead of next measure (center at 0 shift):
        //   gap = BAR_LINE_SHIFT_RATIO * lineSpacing − noteheadRadius * lineSpacing
        //   noteheadRadius ratio ≈ 0.55f (half the notehead width of ~1.1 × lineSpacing).
        // The shift must exceed the notehead radius so the first note does not visually
        // touch the bar line.
        val noteheadRadiusRatio = 0.55f
        assertTrue(
            "BAR_LINE_SHIFT_RATIO ($BAR_LINE_SHIFT_RATIO) must exceed notehead radius ratio ($noteheadRadiusRatio)",
            BAR_LINE_SHIFT_RATIO > noteheadRadiusRatio
        )
        // The shift should be at least 1× notehead-width (= ~1.1 × lineSpacing) so there is a
        // visible gap between bar line and the following notehead edge.
        val noteheadWidthRatio = 1.1f
        assertTrue(
            "BAR_LINE_SHIFT_RATIO ($BAR_LINE_SHIFT_RATIO) should be >= noteheadWidthRatio ($noteheadWidthRatio) for visible after-bar gap",
            BAR_LINE_SHIFT_RATIO >= noteheadWidthRatio
        )
    }

    @Test
    fun `generateExampleGameState produces dynamic content`() {
        val now = 1_700_000_000_000L
        val state = generateExampleGameState(now)

        assertTrue(state.levelTitle.isNotEmpty())
        assertTrue(state.notes.isNotEmpty())
        assertTrue(state.chords.isNotEmpty())
    }

    @Test
    fun `generateExampleGameState bpm field is non-negative`() {
        val state = generateExampleGameState(System.currentTimeMillis())
        assertTrue(state.bpm >= 0f)
    }

    @Test
    fun `classifyStepNotes marks matched missing and extra notes separately`() {
        val outcome = classifyStepNotes(
            expectedNotes = listOf(60),
            playedNotes = listOf(60, 69)
        )

        assertEquals(listOf(NoteState.CORRECT), outcome.expectedStates)
        assertEquals(listOf(69), outcome.extraNotes)
    }

    @Test
    fun `classifyStepNotes preserves expected-note order for mixed matches`() {
        val outcome = classifyStepNotes(
            expectedNotes = listOf(60, 64),
            playedNotes = listOf(64)
        )

        assertEquals(listOf(NoteState.WRONG, NoteState.CORRECT), outcome.expectedStates)
        assertTrue(outcome.extraNotes.isEmpty())
    }

    @Test
    fun `isExpectedPedalSatisfied accepts press when pedal already held`() {
        val snapshot = StepInputSnapshot(
            playedNotes = listOf(60),
            playedPedalAction = PedalAction.NONE,
            inputTimestampMs = 1_500L,
            pedalPressedAtInput = true,
            lastPedalReleaseTimestampMs = null
        )

        assertTrue(isExpectedPedalSatisfied(PedalAction.PRESS, snapshot))
    }

    @Test
    fun `isExpectedPedalSatisfied accepts only recent release lead window`() {
        val recentSnapshot = StepInputSnapshot(
            playedNotes = listOf(60),
            playedPedalAction = PedalAction.NONE,
            inputTimestampMs = 2_700L,
            pedalPressedAtInput = false,
            lastPedalReleaseTimestampMs = 2_000L
        )
        val staleSnapshot = recentSnapshot.copy(inputTimestampMs = 3_500L)

        assertTrue(isExpectedPedalSatisfied(PedalAction.RELEASE, recentSnapshot, releaseLeadToleranceMs = 1_000L))
        assertFalse(isExpectedPedalSatisfied(PedalAction.RELEASE, staleSnapshot, releaseLeadToleranceMs = 1_000L))
    }

    @Test
    fun `formatChordLabel uses key-aware enharmonic root spelling`() {
        val chord = listOf(66, 70, 73) // Gb/F# major pitch classes

        assertEquals("F#M", formatChordLabelShort(chord, musicalKey = 7)) // G major (sharps)
        assertEquals("GbM", formatChordLabelShort(chord, musicalKey = 8)) // Ab major (flats)
    }

    @Test
    fun `resolveDisplayChordNotes maps single note runs to chord notes`() {
        val steps = listOf(
            ExerciseStep(notes = listOf(60)),
            ExerciseStep(notes = listOf(64)),
            ExerciseStep(notes = listOf(67)),
            ExerciseStep(notes = listOf(64))
        )

        val resolved = resolveDisplayChordNotes(steps)

        assertEquals(listOf(60, 64, 67), resolved[0])
        assertEquals(listOf(60, 64, 67), resolved[1])
        assertEquals(listOf(60, 64, 67), resolved[2])
        assertEquals(listOf(60, 64, 67), resolved[3])
    }

    @Test
    fun `resolveDisplayChordNotes keeps multi note step notes unchanged`() {
        val steps = listOf(
            ExerciseStep(notes = listOf(60, 64, 67)),
            ExerciseStep(notes = listOf(62))
        )

        val resolved = resolveDisplayChordNotes(steps)

        assertEquals(listOf(60, 64, 67), resolved[0])
        assertEquals(listOf(62), resolved[1])
    }

    @Test
    fun `resolveDisplayChordNotes splits consecutive detected chords in one run`() {
        val steps = listOf(
            ExerciseStep(notes = listOf(60)),
            ExerciseStep(notes = listOf(64)),
            ExerciseStep(notes = listOf(67)),
            ExerciseStep(notes = listOf(62)),
            ExerciseStep(notes = listOf(65)),
            ExerciseStep(notes = listOf(69))
        )

        val resolved = resolveDisplayChordNotes(steps)

        assertEquals(listOf(60, 64, 67), resolved[0])
        assertEquals(listOf(60, 64, 67), resolved[2])
        assertEquals(listOf(62, 65, 69), resolved[3])
        assertEquals(listOf(62, 65, 69), resolved[5])
    }

    @Test
    fun `resolveDisplayChordNotes keeps isolated melody notes as note labels`() {
        val steps = listOf(
            ExerciseStep(notes = listOf(60)),
            ExerciseStep(notes = listOf(62)),
            ExerciseStep(notes = listOf(65))
        )

        val resolved = resolveDisplayChordNotes(steps)

        assertEquals(listOf(60), resolved[0])
        assertEquals(listOf(62), resolved[1])
        assertEquals(listOf(65), resolved[2])
    }

    @Test
    fun `resolveDisplayChordNotes resolves suspended chord runs`() {
        val steps = listOf(
            ExerciseStep(notes = listOf(62)), // D
            ExerciseStep(notes = listOf(67)), // G
            ExerciseStep(notes = listOf(69))  // A
        )

        val resolved = resolveDisplayChordNotes(steps)

        assertEquals("Gsus2", formatChordLabelShort(resolved[0].orEmpty()))
        assertEquals("Gsus2", formatChordLabelShort(resolved[1].orEmpty()))
        assertEquals("Gsus2", formatChordLabelShort(resolved[2].orEmpty()))
    }

    @Test
    fun `resolveDisplayChordNotes resolves power-chord runs`() {
        val steps = listOf(
            ExerciseStep(notes = listOf(60)), // C
            ExerciseStep(notes = listOf(67))  // G
        )

        val resolved = resolveDisplayChordNotes(steps)

        assertEquals("C5", formatChordLabelShort(resolved[0].orEmpty()))
        assertEquals("C5", formatChordLabelShort(resolved[1].orEmpty()))
    }

    @Test
    fun `buildMeasureChordLabels creates one chord label per measure`() {
        val steps = listOf(
            ExerciseStep(notes = listOf(60)),
            ExerciseStep(notes = listOf(64)),
            ExerciseStep(notes = listOf(67)),
            ExerciseStep(notes = listOf(64))
        )
        val stepBeats = listOf(0f, 2f, 4f, 6f) // all within measure 0

        val labels = buildMeasureChordLabels(
            steps = steps,
            stepBeats = stepBeats,
            handMode = HandMode.RIGHT,
            musicalKey = 0
        )

        assertEquals(1, labels.size)
        assertEquals(0f, labels.first().startBeat)
        assertTrue(labels.first().name.startsWith("CM"))
    }

    @Test
    fun `buildMeasureChordLabels tolerates extra tones in a measure`() {
        val steps = listOf(
            ExerciseStep(notes = listOf(60)),
            ExerciseStep(notes = listOf(64)),
            ExerciseStep(notes = listOf(65)),
            ExerciseStep(notes = listOf(67)),
            ExerciseStep(notes = listOf(71)),
            ExerciseStep(notes = listOf(74))
        )
        val stepBeats = listOf(0f, 1f, 2f, 3f, 4f, 5f)

        val labels = buildMeasureChordLabels(
            steps = steps,
            stepBeats = stepBeats,
            handMode = HandMode.RIGHT,
            musicalKey = 0
        )

        assertEquals(1, labels.size)
        assertTrue(labels.first().name.startsWith("CM11"))
    }

    @Test
    fun `buildMeasureChordLabels omits unresolved measures instead of note-name fallback`() {
        val steps = listOf(
            ExerciseStep(notes = listOf(60)),
            ExerciseStep(notes = listOf(61))
        )
        val stepBeats = listOf(0f, 2f)

        val labels = buildMeasureChordLabels(
            steps = steps,
            stepBeats = stepBeats,
            handMode = HandMode.RIGHT,
            musicalKey = 0
        )

        assertTrue(labels.isEmpty())
    }
}
