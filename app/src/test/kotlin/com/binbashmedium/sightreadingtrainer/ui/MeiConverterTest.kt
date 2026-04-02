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

import com.binbashmedium.sightreadingtrainer.domain.model.NoteAccidental
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeiConverterTest {

    // ── midiToMeiPitch ────────────────────────────────────────────────────────

    @Test
    fun `C4 maps to pname c oct 4 no accidental`() {
        val (pname, oct, accid) = MeiConverter.midiToMeiPitch(60, NoteAccidental.NONE)
        assertEquals("c", pname)
        assertEquals(4, oct)
        assertNull(accid)
    }

    @Test
    fun `C5 maps to pname c oct 5`() {
        val (pname, oct, _) = MeiConverter.midiToMeiPitch(72, NoteAccidental.NONE)
        assertEquals("c", pname)
        assertEquals(5, oct)
    }

    @Test
    fun `D4 maps to pname d oct 4 no accidental`() {
        val (pname, oct, accid) = MeiConverter.midiToMeiPitch(62, NoteAccidental.NONE)
        assertEquals("d", pname)
        assertEquals(4, oct)
        assertNull(accid)
    }

    @Test
    fun `C-sharp maps to c with sharp accid-ges`() {
        val (pname, oct, accid) = MeiConverter.midiToMeiPitch(61, NoteAccidental.SHARP)
        assertEquals("c", pname)
        assertEquals(4, oct)
        assertEquals("s", accid)
    }

    @Test
    fun `D-flat maps to d with flat accid-ges`() {
        val (pname, oct, accid) = MeiConverter.midiToMeiPitch(61, NoteAccidental.FLAT)
        assertEquals("d", pname)
        assertEquals(4, oct)
        assertEquals("f", accid)
    }

    @Test
    fun `E4 maps to pname e oct 4 no accidental`() {
        val (pname, oct, accid) = MeiConverter.midiToMeiPitch(64, NoteAccidental.NONE)
        assertEquals("e", pname)
        assertEquals(4, oct)
        assertNull(accid)
    }

    @Test
    fun `F4 maps to pname f oct 4 no accidental`() {
        val (pname, oct, accid) = MeiConverter.midiToMeiPitch(65, NoteAccidental.NONE)
        assertEquals("f", pname)
        assertEquals(4, oct)
        assertNull(accid)
    }

    @Test
    fun `G4 maps to pname g oct 4 no accidental`() {
        val (pname, oct, accid) = MeiConverter.midiToMeiPitch(67, NoteAccidental.NONE)
        assertEquals("g", pname)
        assertEquals(4, oct)
        assertNull(accid)
    }

    @Test
    fun `A4 maps to pname a oct 4 no accidental`() {
        val (pname, oct, accid) = MeiConverter.midiToMeiPitch(69, NoteAccidental.NONE)
        assertEquals("a", pname)
        assertEquals(4, oct)
        assertNull(accid)
    }

    @Test
    fun `B4 maps to pname b oct 4 no accidental`() {
        val (pname, oct, accid) = MeiConverter.midiToMeiPitch(71, NoteAccidental.NONE)
        assertEquals("b", pname)
        assertEquals(4, oct)
        assertNull(accid)
    }

    @Test
    fun `G-sharp maps to g with sharp accid-ges`() {
        val (pname, _, accid) = MeiConverter.midiToMeiPitch(68, NoteAccidental.SHARP)
        assertEquals("g", pname)
        assertEquals("s", accid)
    }

    @Test
    fun `A-flat maps to a with flat accid-ges`() {
        val (pname, _, accid) = MeiConverter.midiToMeiPitch(68, NoteAccidental.FLAT)
        assertEquals("a", pname)
        assertEquals("f", accid)
    }

    @Test
    fun `B3 maps to pname b oct 3`() {
        val (pname, oct, accid) = MeiConverter.midiToMeiPitch(59, NoteAccidental.NONE)
        assertEquals("b", pname)
        assertEquals(3, oct)
        assertNull(accid)
    }

    @Test
    fun `middle C MIDI 60 is oct 4`() {
        val (_, oct, _) = MeiConverter.midiToMeiPitch(60, NoteAccidental.NONE)
        assertEquals(4, oct)
    }

    // ── quarterBeatsToDur ─────────────────────────────────────────────────────

    @Test
    fun `whole note 4 quarter beats maps to dur 1`() {
        assertEquals("1", MeiConverter.quarterBeatsToDur(4f))
    }

    @Test
    fun `half note 2 quarter beats maps to dur 2`() {
        assertEquals("2", MeiConverter.quarterBeatsToDur(2f))
    }

    @Test
    fun `quarter note 1 beat maps to dur 4`() {
        assertEquals("4", MeiConverter.quarterBeatsToDur(1f))
    }

    @Test
    fun `eighth note 0-5 beats maps to dur 8`() {
        assertEquals("8", MeiConverter.quarterBeatsToDur(0.5f))
    }

    @Test
    fun `sixteenth note 0-25 beats maps to dur 16`() {
        assertEquals("16", MeiConverter.quarterBeatsToDur(0.25f))
    }

    // ── keySignatureAttr ──────────────────────────────────────────────────────

    @Test
    fun `C major key 0 produces key-sig 0`() {
        assertEquals("key.sig=\"0\"", MeiConverter.keySignatureAttr(0))
    }

    @Test
    fun `G major key 7 produces 1 sharp`() {
        assertEquals("key.sig=\"1s\"", MeiConverter.keySignatureAttr(7))
    }

    @Test
    fun `D major key 2 produces 2 sharps`() {
        assertEquals("key.sig=\"2s\"", MeiConverter.keySignatureAttr(2))
    }

    @Test
    fun `F major key 5 produces 1 flat`() {
        assertEquals("key.sig=\"1f\"", MeiConverter.keySignatureAttr(5))
    }

    @Test
    fun `Bb major key 10 produces 2 flats`() {
        assertEquals("key.sig=\"2f\"", MeiConverter.keySignatureAttr(10))
    }

    @Test
    fun `C-sharp major key 1 produces 7 sharps`() {
        assertEquals("key.sig=\"7s\"", MeiConverter.keySignatureAttr(1))
    }

    // ── noteStateMeiColor ─────────────────────────────────────────────────────

    @Test
    fun `NONE state returns null color`() {
        assertNull(MeiConverter.noteStateMeiColor(NoteState.NONE))
    }

    @Test
    fun `CORRECT state returns green`() {
        assertEquals("#2E7D32", MeiConverter.noteStateMeiColor(NoteState.CORRECT))
    }

    @Test
    fun `WRONG state returns red`() {
        assertEquals("#C62828", MeiConverter.noteStateMeiColor(NoteState.WRONG))
    }

    @Test
    fun `LATE state returns yellow`() {
        assertEquals("#F9A825", MeiConverter.noteStateMeiColor(NoteState.LATE))
    }

    // ── renderLayer ───────────────────────────────────────────────────────────

    @Test
    fun `empty note list produces mRest`() {
        val result = MeiConverter.renderLayer(emptyList(), 0f, -1f)
        assertEquals("<mRest/>", result)
    }

    @Test
    fun `single quarter note fills measure with no rest`() {
        val note = NoteEvent(midi = 60, startBeat = 0f, duration = 4f,
            expected = true, state = NoteState.NONE, staff = StaffType.TREBLE)
        val result = MeiConverter.renderLayer(listOf(note), 0f, -1f)
        assertFalse("Should not contain rest", result.contains("<rest"))
        assertTrue("Should contain note", result.contains("<note"))
        assertTrue("Should use dur 1 (whole)", result.contains("dur=\"1\""))
    }

    @Test
    fun `quarter note at measure start fills tail with rest`() {
        val note = NoteEvent(midi = 60, startBeat = 0f, duration = 1f,
            expected = true, state = NoteState.NONE, staff = StaffType.TREBLE)
        val result = MeiConverter.renderLayer(listOf(note), 0f, -1f)
        assertTrue("Should contain note", result.contains("<note"))
        assertTrue("Should contain rest for remaining 3 beats", result.contains("<rest"))
    }

    @Test
    fun `note at beat offset has leading rest`() {
        // Note at UI beat 4 = qBeat 2 → 2 quarter rests before it
        val note = NoteEvent(midi = 60, startBeat = 4f, duration = 2f,
            expected = true, state = NoteState.NONE, staff = StaffType.TREBLE)
        val result = MeiConverter.renderLayer(listOf(note), 0f, -1f)
        assertTrue("Should have leading rest", result.indexOf("<rest") < result.indexOf("<note"))
    }

    @Test
    fun `current beat note gets ncurr id prefix`() {
        val note = NoteEvent(midi = 60, startBeat = 0f, duration = 1f,
            expected = true, state = NoteState.NONE, staff = StaffType.TREBLE)
        val result = MeiConverter.renderLayer(listOf(note), 0f, 0f) // currentBeat = 0
        assertTrue("Current note should have ncurr prefix", result.contains("ncurr"))
    }

    @Test
    fun `non-current note gets nb id prefix`() {
        val note = NoteEvent(midi = 60, startBeat = 0f, duration = 1f,
            expected = true, state = NoteState.NONE, staff = StaffType.TREBLE)
        val result = MeiConverter.renderLayer(listOf(note), 0f, 99f) // currentBeat elsewhere
        assertTrue("Non-current note should have nb prefix", result.contains("nb"))
        assertFalse("Should not have ncurr prefix", result.contains("ncurr"))
    }

    @Test
    fun `CORRECT note has green color attribute`() {
        val note = NoteEvent(midi = 64, startBeat = 0f, duration = 1f,
            expected = true, state = NoteState.CORRECT, staff = StaffType.TREBLE)
        val result = MeiConverter.renderLayer(listOf(note), 0f, -1f)
        assertTrue("Correct note should have green color", result.contains("#2E7D32"))
    }

    @Test
    fun `WRONG note has red color attribute`() {
        val note = NoteEvent(midi = 64, startBeat = 0f, duration = 1f,
            expected = true, state = NoteState.WRONG, staff = StaffType.TREBLE)
        val result = MeiConverter.renderLayer(listOf(note), 0f, -1f)
        assertTrue("Wrong note should have red color", result.contains("#C62828"))
    }

    @Test
    fun `NONE note has no color attribute`() {
        val note = NoteEvent(midi = 64, startBeat = 0f, duration = 1f,
            expected = true, state = NoteState.NONE, staff = StaffType.TREBLE)
        val result = MeiConverter.renderLayer(listOf(note), 0f, -1f)
        assertFalse("NONE note should not have color attribute", result.contains("color="))
    }

    @Test
    fun `two notes at same beat produce chord element`() {
        val n1 = NoteEvent(midi = 60, startBeat = 0f, duration = 1f,
            expected = true, state = NoteState.NONE, staff = StaffType.TREBLE)
        val n2 = NoteEvent(midi = 64, startBeat = 0f, duration = 1f,
            expected = true, state = NoteState.NONE, staff = StaffType.TREBLE)
        val result = MeiConverter.renderLayer(listOf(n1, n2), 0f, -1f)
        assertTrue("Two simultaneous notes should form a chord", result.contains("<chord"))
    }

    // ── convert (integration) ─────────────────────────────────────────────────

    @Test
    fun `convert produces valid MEI document`() {
        val gameState = generateExampleGameState()
        val mei = MeiConverter.convert(gameState, 0f, BEATS_PER_ROW)
        assertTrue("Should start with XML declaration", mei.startsWith("<?xml"))
        assertTrue("Should contain mei element", mei.contains("<mei "))
        assertTrue("Should contain staffDef for treble", mei.contains("clef.shape=\"G\""))
        assertTrue("Should contain staffDef for bass", mei.contains("clef.shape=\"F\""))
        assertTrue("Should contain section", mei.contains("<section>"))
        assertTrue("Should contain at least one measure", mei.contains("<measure"))
    }

    @Test
    fun `convert respects beat range - excludes notes outside range`() {
        val note = NoteEvent(midi = 60, startBeat = 40f, duration = 1f,
            expected = true, state = NoteState.NONE, staff = StaffType.TREBLE)
        val gameState = GameState(
            levelTitle = "Test",
            elapsedTime = 0L,
            score = 0,
            bpm = 0f,
            notes = listOf(note),
            chords = emptyList(),
            pedalMarks = emptyList(),
            currentBeat = 0f
        )
        // Only render beats 0-32: note at beat 40 should be excluded
        val mei = MeiConverter.convert(gameState, 0f, BEATS_PER_ROW)
        // All measures in 0-32 should be empty (mRest)
        assertTrue("Notes outside range should not appear", mei.contains("mRest"))
    }

    @Test
    fun `key signature G major produces 1 sharp in MEI`() {
        val gameState = GameState(
            levelTitle = "Test",
            elapsedTime = 0L,
            score = 0,
            bpm = 0f,
            notes = emptyList(),
            chords = emptyList(),
            pedalMarks = emptyList(),
            currentBeat = 0f,
            musicalKey = 7  // G major = 1 sharp
        )
        val mei = MeiConverter.convert(gameState, 0f, BEATS_PER_ROW)
        assertTrue("G major should have 1s key signature", mei.contains("key.sig=\"1s\""))
    }

    @Test
    fun `natural accidental produces accid-n in note`() {
        val note = NoteEvent(midi = 64, startBeat = 0f, duration = 1f,
            expected = true, state = NoteState.NONE, staff = StaffType.TREBLE,
            accidental = NoteAccidental.NATURAL)
        val result = MeiConverter.renderLayer(listOf(note), 0f, -1f)
        assertTrue("Natural accidental should produce accid=n", result.contains("accid=\"n\""))
    }
}
