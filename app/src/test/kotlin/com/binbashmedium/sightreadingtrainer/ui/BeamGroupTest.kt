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
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [computeBeamGroups]. */
class BeamGroupTest {

    // MIDI note numbers used in tests
    // Middle C = 60, D4=62, E4=64, F4=65, G4=67, A4=69
    private val C4 = 60
    private val E4 = 64
    private val G4 = 67

    private fun eighth(midi: Int, startBeat: Float, staff: StaffType = StaffType.TREBLE): NoteEvent =
        NoteEvent(midi = midi, startBeat = startBeat, duration = 0.5f, expected = true, staff = staff)

    private fun quarter(midi: Int, startBeat: Float, staff: StaffType = StaffType.TREBLE): NoteEvent =
        NoteEvent(midi = midi, startBeat = startBeat, duration = 1f, expected = true, staff = staff)

    @Test
    fun `empty note list produces no beam groups`() {
        val groups = computeBeamGroups(emptyList())
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `quarter notes produce no beam groups`() {
        val notes = listOf(
            quarter(C4, 0f),
            quarter(E4, 2f),
            quarter(G4, 4f),
            quarter(C4, 6f)
        )
        val groups = computeBeamGroups(notes)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `single isolated eighth note produces no beam group`() {
        // Only one eighth note on a given quarter-beat — no group
        val notes = listOf(eighth(C4, 0f))
        val groups = computeBeamGroups(notes)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `two eighths on same quarter-beat form one beam group`() {
        // Beat 0 and 1 are both in quarter-beat 0 (0 and 1 / BEATS_PER_STEP=2 → index 0)
        val notes = listOf(eighth(C4, 0f), eighth(E4, 1f))
        val groups = computeBeamGroups(notes)
        assertEquals(1, groups.size)
        assertEquals(listOf(0f, 1f), groups[0].beatPositions)
        assertEquals(StaffType.TREBLE, groups[0].staff)
    }

    @Test
    fun `one measure of eight eighths produces four beam groups`() {
        // Beats 0..7, quarter-beat indices 0,0,1,1,2,2,3,3
        val notes = (0 until 8).map { i -> eighth(C4, i.toFloat()) }
        val groups = computeBeamGroups(notes)
        assertEquals(4, groups.size)
        assertEquals(listOf(0f, 1f), groups[0].beatPositions)
        assertEquals(listOf(2f, 3f), groups[1].beatPositions)
        assertEquals(listOf(4f, 5f), groups[2].beatPositions)
        assertEquals(listOf(6f, 7f), groups[3].beatPositions)
    }

    @Test
    fun `beam groups on different staves are independent`() {
        val trebleNotes = listOf(eighth(C4, 0f, StaffType.TREBLE), eighth(E4, 1f, StaffType.TREBLE))
        val bassNotes = listOf(
            eighth(48, 0f, StaffType.BASS),  // C3
            eighth(50, 1f, StaffType.BASS)   // D3
        )
        val groups = computeBeamGroups(trebleNotes + bassNotes)
        assertEquals(2, groups.size)
        val staffs = groups.map { it.staff }.toSet()
        assertTrue(staffs.contains(StaffType.TREBLE))
        assertTrue(staffs.contains(StaffType.BASS))
    }

    @Test
    fun `mixing eighth and quarter notes only beams the eighths`() {
        val notes = listOf(
            quarter(C4, 0f),
            eighth(E4, 2f),
            eighth(G4, 3f),
            quarter(C4, 4f)
        )
        val groups = computeBeamGroups(notes)
        assertEquals(1, groups.size)
        assertEquals(listOf(2f, 3f), groups[0].beatPositions)
    }

    @Test
    fun `stem direction UP when all notes below middle line`() {
        // Notes below treble middle line (step 34) → stem UP
        // MIDI 57 = A3 (below middle line of treble staff)
        val notes = listOf(eighth(57, 0f), eighth(57, 1f))
        val groups = computeBeamGroups(notes)
        assertEquals(1, groups.size)
        assertEquals(StemDirection.UP, groups[0].direction)
    }

    @Test
    fun `stem direction DOWN when all notes above middle line`() {
        // Notes above treble middle line → stem DOWN
        // MIDI 72 = C5 (above middle line of treble staff)
        val notes = listOf(eighth(72, 0f), eighth(72, 1f))
        val groups = computeBeamGroups(notes)
        assertEquals(1, groups.size)
        assertEquals(StemDirection.DOWN, groups[0].direction)
    }

    @Test
    fun `beam group direction is consistent across mixed notes in the group`() {
        // Both notes above middle line; farthest note determines direction.
        // MIDI 81 = A5 (step 40, far above treble middle B4=step 34)
        // MIDI 72 = C5 (step 35, just above middle)
        // highestDistance=6, lowestDistance=-1 → DOWN
        val notes = listOf(eighth(81, 0f), eighth(72, 1f))
        val groups = computeBeamGroups(notes)
        assertEquals(1, groups.size)
        assertEquals(StemDirection.DOWN, groups[0].direction)
    }

    @Test
    fun `beat positions are sorted ascending within each group`() {
        // Add notes out of order to verify sorting
        val notes = listOf(eighth(C4, 1f), eighth(E4, 0f))
        val groups = computeBeamGroups(notes)
        assertEquals(1, groups.size)
        assertEquals(listOf(0f, 1f), groups[0].beatPositions)
    }

    @Test
    fun `two measures of eighths produce eight beam groups`() {
        // 16 eighth notes across 2 measures (beats 0-15)
        val notes = (0 until 16).map { i -> eighth(C4, i.toFloat()) }
        val groups = computeBeamGroups(notes)
        assertEquals(8, groups.size)
    }

    @Test
    fun `chord of two eighths at same beat counts as one chord position`() {
        // Two notes at beat=0, one note at beat=1 — still just 2 distinct beat positions
        val notes = listOf(
            eighth(C4, 0f),
            eighth(E4, 0f),  // chord at beat 0
            eighth(G4, 1f)
        )
        val groups = computeBeamGroups(notes)
        assertEquals(1, groups.size)
        assertEquals(listOf(0f, 1f), groups[0].beatPositions)
    }
}
