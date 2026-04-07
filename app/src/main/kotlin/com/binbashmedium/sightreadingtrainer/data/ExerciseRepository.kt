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
import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseInputSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    generatedExerciseSource: GeneratedExerciseSource,
    databaseExerciseSource: DatabaseExerciseSource
) {
    private val sourcesByType: Map<ExerciseInputSource, ExerciseSource> =
        listOf(generatedExerciseSource, databaseExerciseSource).associateBy { it.sourceType }

    fun generateExercise(settings: AppSettings, forcedKey: Int? = null): Exercise {
        val selectedSource = sourcesByType[settings.exerciseInputSource]
            ?: sourcesByType.getValue(ExerciseInputSource.GENERATED)
        val selectedResult = selectedSource.load(settings, forcedKey)
        if (selectedResult.steps.isNotEmpty() || settings.exerciseInputSource == ExerciseInputSource.GENERATED) {
            return selectedResult
        }
        return sourcesByType.getValue(ExerciseInputSource.GENERATED).load(settings, forcedKey)
    }
}
