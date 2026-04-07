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
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.usecase.GenerateExerciseUseCase
import javax.inject.Inject
import javax.inject.Singleton

interface ExerciseSource {
    val sourceType: ExerciseInputSource
    fun load(settings: AppSettings, forcedKey: Int? = null): Exercise
}

@Singleton
class GeneratedExerciseSource @Inject constructor(
    private val generateExerciseUseCase: GenerateExerciseUseCase
) : ExerciseSource {
    override val sourceType: ExerciseInputSource = ExerciseInputSource.GENERATED

    override fun load(settings: AppSettings, forcedKey: Int?): Exercise =
        generateExerciseUseCase.execute(settings, forcedKey)
}

/** Database-backed source that maps storage models to domain [Exercise]. */
@Singleton
class DatabaseExerciseSource @Inject constructor(
    private val exerciseLibraryRepository: ExerciseLibraryRepository
) : ExerciseSource {
    override val sourceType: ExerciseInputSource = ExerciseInputSource.DATABASE

    override fun load(settings: AppSettings, forcedKey: Int?): Exercise {
        val key = (forcedKey ?: settings.selectedKeys.firstOrNull() ?: 0).coerceIn(0, 11)
        val candidates = exerciseLibraryRepository.listExercises()
            .filter { it.handMode == settings.handMode || settings.handMode == HandMode.BOTH }
        val selected = candidates.firstOrNull { it.key == key } ?: candidates.firstOrNull()
        return if (selected != null) {
            StoredExerciseMapper.toDomain(selected)
        } else {
            // Repository empty -> graceful fallback to generated source semantics.
            Exercise(steps = emptyList(), musicalKey = key, handMode = settings.handMode)
        }
    }
}
