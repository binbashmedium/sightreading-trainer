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

package com.binbashmedium.sightreadingtrainer.data

import com.binbashmedium.sightreadingtrainer.domain.model.ChordProgression
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDataStoreProgressionsTest {

    @Test
    fun `parseSelectedProgressions falls back to default when input is null or invalid`() {
        assertEquals(
            setOf(ChordProgression.I_IV_V_I),
            parseSelectedProgressions(null)
        )
        assertEquals(
            setOf(ChordProgression.I_IV_V_I),
            parseSelectedProgressions("NOT_A_PROGRESSION")
        )
    }

    @Test
    fun `serializeProgressions writes ordinal order and parseSelectedProgressions restores it`() {
        val raw = serializeProgressions(
            setOf(ChordProgression.II_V_I, ChordProgression.I_IV_V_I, ChordProgression.I_III_IV_V)
        )

        assertEquals("I_IV_V_I,II_V_I,I_III_IV_V", raw)
        assertEquals(
            setOf(ChordProgression.I_IV_V_I, ChordProgression.II_V_I, ChordProgression.I_III_IV_V),
            parseSelectedProgressions(raw)
        )
    }
}
