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

import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseStep
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteValue
import com.binbashmedium.sightreadingtrainer.domain.model.OrnamentType
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage-facing schema for pre-authored exercises.
 * This can be mapped from SQL/JSON/MusicXML import rows before entering the domain pipeline.
 */
data class StoredExercise(
    val id: String,
    val title: String,
    val key: Int,
    val handMode: HandMode,
    val steps: List<StoredExerciseStep>
)

data class StoredExerciseStep(
    val midiNotes: List<Int>,
    val noteValue: NoteValue = NoteValue.QUARTER,
    val pedalAction: PedalAction = PedalAction.NONE,
    val ornament: OrnamentType = OrnamentType.NONE
)

interface ExerciseLibraryRepository {
    fun listExercises(): List<StoredExercise>
}

@Singleton
class InMemoryExerciseLibraryRepository @Inject constructor() : ExerciseLibraryRepository {
    override fun listExercises(): List<StoredExercise> = EXERCISES

    private companion object {
        val EXERCISES = listOf(
            StoredExercise(
                id = "db-c-major-cadence-rh",
                title = "C Major Cadence RH",
                key = 0,
                handMode = HandMode.RIGHT,
                steps = listOf(
                    StoredExerciseStep(listOf(60, 64, 67), NoteValue.QUARTER),
                    StoredExerciseStep(listOf(65, 69, 72), NoteValue.QUARTER),
                    StoredExerciseStep(listOf(67, 71, 74), NoteValue.QUARTER, PedalAction.PRESS),
                    StoredExerciseStep(listOf(60, 64, 67), NoteValue.QUARTER, PedalAction.RELEASE)
                )
            ),
            StoredExercise(
                id = "db-a-minor-melody-lh",
                title = "A Minor Melody LH",
                key = 9,
                handMode = HandMode.LEFT,
                steps = listOf(
                    StoredExerciseStep(listOf(45), NoteValue.HALF),
                    StoredExerciseStep(listOf(48), NoteValue.QUARTER, ornament = OrnamentType.APPOGGIATURA),
                    StoredExerciseStep(listOf(52), NoteValue.QUARTER),
                    StoredExerciseStep(listOf(57), NoteValue.HALF)
                )
            )
        )
    }
}

internal object StoredExerciseMapper {
    fun toDomain(stored: StoredExercise): Exercise = Exercise(
        steps = stored.steps.map { step ->
            ExerciseStep(
                notes = step.midiNotes,
                noteValue = step.noteValue,
                pedalAction = step.pedalAction,
                ornament = step.ornament
            )
        },
        musicalKey = stored.key.coerceIn(0, 11),
        handMode = stored.handMode
    )
}
