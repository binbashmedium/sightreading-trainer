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

import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseInputSource
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseMode
import com.binbashmedium.sightreadingtrainer.domain.model.OrnamentType
import com.binbashmedium.sightreadingtrainer.domain.model.ProgressionExerciseType
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDataStoreParsingTest {

    @Test
    fun `parseExerciseInputSource trims whitespace and falls back to generated`() {
        assertEquals(
            ExerciseInputSource.DATABASE,
            parseExerciseInputSource(" DATABASE ")
        )
        assertEquals(
            ExerciseInputSource.GENERATED,
            parseExerciseInputSource("unknown")
        )
        assertEquals(
            ExerciseInputSource.GENERATED,
            parseExerciseInputSource(null)
        )
    }

    @Test
    fun `parseSelectedOrnaments ignores NONE and invalid entries`() {
        assertEquals(
            setOf(OrnamentType.TRILL, OrnamentType.UPPER_MORDENT),
            parseSelectedOrnaments("TRILL,NONE,UPPER_MORDENT,INVALID")
        )
        assertEquals(emptySet<OrnamentType>(), parseSelectedOrnaments(null))
    }

    @Test
    fun `parseExerciseMode explicit value wins over migrated legacy type`() {
        assertEquals(
            ExerciseMode.CLASSIC,
            parseExerciseMode("CLASSIC", setOf(ExerciseContentType.PROGRESSIONS))
        )
        assertEquals(
            ExerciseMode.PROGRESSIONS,
            parseExerciseMode("PROGRESSIONS", emptySet())
        )
    }

    @Test
    fun `parseProgressionExerciseTypes uses explicit value and ignores invalid tokens`() {
        assertEquals(
            setOf(ProgressionExerciseType.NINTHS, ProgressionExerciseType.ARPEGGIOS),
            parseProgressionExerciseTypes("NINTHS,INVALID,ARPEGGIOS", emptySet())
        )
    }
}
