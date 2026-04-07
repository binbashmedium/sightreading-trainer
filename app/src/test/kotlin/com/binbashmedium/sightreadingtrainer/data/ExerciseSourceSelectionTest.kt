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

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseInputSource
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteValue
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import com.binbashmedium.sightreadingtrainer.domain.usecase.GenerateExerciseUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSourceSelectionTest {

    @Test
    fun `parseExerciseInputSource falls back to GENERATED for null or unknown`() {
        assertEquals(ExerciseInputSource.GENERATED, parseExerciseInputSource(null))
        assertEquals(ExerciseInputSource.GENERATED, parseExerciseInputSource("UNKNOWN"))
    }

    @Test
    fun `repository uses database source when configured`() {
        val repository = ExerciseRepository(
            generatedExerciseSource = GeneratedExerciseSource(GenerateExerciseUseCase()),
            databaseExerciseSource = DatabaseExerciseSource(InMemoryExerciseLibraryRepository())
        )
        val settings = AppSettings(
            exerciseInputSource = ExerciseInputSource.DATABASE,
            handMode = HandMode.RIGHT
        )

        val exercise = repository.generateExercise(settings, forcedKey = 0)

        assertTrue(exercise.steps.isNotEmpty())
        assertEquals(ExerciseInputSource.DATABASE, settings.exerciseInputSource)
        assertEquals(HandMode.RIGHT, exercise.handMode)
        assertEquals(PedalAction.PRESS, exercise.steps[2].pedalAction)
        assertEquals(PedalAction.RELEASE, exercise.steps[3].pedalAction)
    }

    @Test
    fun `database source respects hand-mode filtering`() {
        val source = DatabaseExerciseSource(InMemoryExerciseLibraryRepository())
        val settings = AppSettings(exerciseInputSource = ExerciseInputSource.DATABASE, handMode = HandMode.LEFT)

        val exercise = source.load(settings, forcedKey = 9)

        assertEquals(HandMode.LEFT, exercise.handMode)
        assertTrue(exercise.steps.all { it.notes.isNotEmpty() })
    }

    @Test
    fun `repository falls back to generated source when database has no exercises`() {
        val emptyLibrarySource = DatabaseExerciseSource(
            exerciseLibraryRepository = object : ExerciseLibraryRepository {
                override fun listExercises(): List<StoredExercise> = emptyList()
            }
        )
        val repository = ExerciseRepository(
            generatedExerciseSource = GeneratedExerciseSource(GenerateExerciseUseCase()),
            databaseExerciseSource = emptyLibrarySource
        )
        val generatedSettings = AppSettings(
            exerciseInputSource = ExerciseInputSource.DATABASE,
            exerciseTypes = setOf(ExerciseContentType.SINGLE_NOTES),
            selectedNoteValues = setOf(NoteValue.QUARTER)
        )

        val exercise = repository.generateExercise(generatedSettings, forcedKey = 0)

        assertTrue(exercise.steps.isNotEmpty())
    }
}
