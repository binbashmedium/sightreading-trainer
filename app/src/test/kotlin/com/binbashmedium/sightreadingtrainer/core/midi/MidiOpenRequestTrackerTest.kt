package com.binbashmedium.sightreadingtrainer.core.midi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MidiOpenRequestTrackerTest {

    @Test
    fun `new request invalidates older request id`() {
        val tracker = MidiOpenRequestTracker()

        val first = tracker.newRequest()
        val second = tracker.newRequest()

        assertFalse(tracker.isCurrent(first))
        assertTrue(tracker.isCurrent(second))
    }

    @Test
    fun `invalidate marks pending request as stale`() {
        val tracker = MidiOpenRequestTracker()

        val requestId = tracker.newRequest()
        tracker.invalidate()

        assertFalse(tracker.isCurrent(requestId))
    }
}
