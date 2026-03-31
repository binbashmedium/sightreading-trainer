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

import com.binbashmedium.sightreadingtrainer.core.midi.ChordDetector
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordDetectorTest {

    @Test
    fun `single note emitted as single-note chord`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val detector = ChordDetector(chordWindowMs = 50L, scope = scope)
        val receivedChords = mutableListOf<List<Int>>()

        val collectJob = launch(testDispatcher) {
            detector.chords.collect { input ->
                receivedChords.add(input.notes.map { it.midiNote })
            }
        }

        detector.onNoteOn(60, 100)
        advanceTimeBy(100)

        collectJob.cancel()
        scope.cancel()

        assertEquals(1, receivedChords.size)
        assertEquals(listOf(60), receivedChords[0])
    }

    @Test
    fun `simultaneous notes grouped into one chord`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val detector = ChordDetector(chordWindowMs = 50L, scope = scope)
        val receivedChords = mutableListOf<List<Int>>()

        val collectJob = launch(testDispatcher) {
            detector.chords.collect { input ->
                receivedChords.add(input.notes.map { it.midiNote }.sorted())
            }
        }

        detector.onNoteOn(60, 100)
        detector.onNoteOn(64, 100)
        detector.onNoteOn(67, 100)
        advanceTimeBy(100)

        collectJob.cancel()
        scope.cancel()

        assertEquals(1, receivedChords.size)
        assertEquals(listOf(60, 64, 67), receivedChords[0])
    }

    @Test
    fun `notes separated beyond window emitted as separate chords`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val detector = ChordDetector(chordWindowMs = 50L, scope = scope)
        val receivedChords = mutableListOf<List<Int>>()

        val collectJob = launch(testDispatcher) {
            detector.chords.collect { input ->
                receivedChords.add(input.notes.map { it.midiNote })
            }
        }

        detector.onNoteOn(60, 100)
        advanceTimeBy(100)
        detector.onNoteOn(62, 100)
        advanceTimeBy(100)

        collectJob.cancel()
        scope.cancel()

        assertEquals(2, receivedChords.size)
        assertEquals(listOf(60), receivedChords[0])
        assertEquals(listOf(62), receivedChords[1])
    }

    @Test
    fun `reset clears pending notes and no chord is emitted`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val detector = ChordDetector(chordWindowMs = 50L, scope = scope)
        val receivedChords = mutableListOf<List<Int>>()

        val collectJob = launch(testDispatcher) {
            detector.chords.collect { input ->
                receivedChords.add(input.notes.map { it.midiNote })
            }
        }

        detector.onNoteOn(60, 100)
        detector.reset()
        advanceTimeBy(100)

        collectJob.cancel()
        scope.cancel()

        assertTrue(receivedChords.isEmpty())
    }

    @Test
    fun `pedal event can be emitted without notes`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val detector = ChordDetector(chordWindowMs = 50L, scope = scope)
        val receivedPedals = mutableListOf<PedalAction>()

        val collectJob = launch(testDispatcher) {
            detector.chords.collect { input ->
                receivedPedals.add(input.pedalAction)
            }
        }

        detector.onPedalChange(PedalAction.PRESS)
        advanceTimeBy(100)

        collectJob.cancel()
        scope.cancel()

        assertEquals(listOf(PedalAction.PRESS), receivedPedals)
    }

    @Test
    fun `notes and pedal within window are grouped together`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val detector = ChordDetector(chordWindowMs = 50L, scope = scope)
        val received = mutableListOf<Pair<List<Int>, PedalAction>>()

        val collectJob = launch(testDispatcher) {
            detector.chords.collect { input ->
                received.add(input.notes.map { it.midiNote }.sorted() to input.pedalAction)
            }
        }

        detector.onNoteOn(60, 100)
        detector.onPedalChange(PedalAction.PRESS)
        advanceTimeBy(100)

        collectJob.cancel()
        scope.cancel()

        assertEquals(1, received.size)
        assertEquals(listOf(60), received.first().first)
        assertEquals(PedalAction.PRESS, received.first().second)
    }
}
