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
