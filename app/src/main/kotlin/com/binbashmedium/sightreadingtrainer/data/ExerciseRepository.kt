package com.binbashmedium.sightreadingtrainer.data

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.usecase.GenerateExerciseUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val generateExerciseUseCase: GenerateExerciseUseCase
) {
    fun generateExercise(settings: AppSettings): Exercise =
        generateExerciseUseCase.execute(settings)
}
