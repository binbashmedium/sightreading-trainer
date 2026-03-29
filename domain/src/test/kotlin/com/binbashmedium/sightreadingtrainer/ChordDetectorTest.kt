package com.binbashmedium.sightreadingtrainer

import com.binbashmedium.sightreadingtrainer.core.midi.ChordDetector
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
            detector.chords.collect { chord ->
                receivedChords.add(chord.map { it.midiNote })
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
            detector.chords.collect { chord ->
                receivedChords.add(chord.map { it.midiNote }.sorted())
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
            detector.chords.collect { chord ->
                receivedChords.add(chord.map { it.midiNote })
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
            detector.chords.collect { chord ->
                receivedChords.add(chord.map { it.midiNote })
            }
        }

        detector.onNoteOn(60, 100)
        detector.reset()
        advanceTimeBy(100)

        collectJob.cancel()
        scope.cancel()

        assertTrue("No chord should be emitted after reset", receivedChords.isEmpty())
    }
}
