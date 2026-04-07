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

package com.binbashmedium.sightreadingtrainer.di

import com.binbashmedium.sightreadingtrainer.data.ExerciseLibraryRepository
import com.binbashmedium.sightreadingtrainer.data.InMemoryExerciseLibraryRepository
import com.binbashmedium.sightreadingtrainer.domain.usecase.GenerateExerciseUseCase
import com.binbashmedium.sightreadingtrainer.domain.usecase.MatchNotesUseCase
import com.binbashmedium.sightreadingtrainer.domain.usecase.PracticeSessionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMatchNotesUseCase(): MatchNotesUseCase = MatchNotesUseCase()

    @Provides
    @Singleton
    fun provideGenerateExerciseUseCase(): GenerateExerciseUseCase = GenerateExerciseUseCase()

    @Provides
    @Singleton
    fun providePracticeSessionUseCase(
        matchNotesUseCase: MatchNotesUseCase
    ): PracticeSessionUseCase = PracticeSessionUseCase(matchNotesUseCase)

    @Provides
    @Singleton
    fun provideExerciseLibraryRepository(
        inMemoryExerciseLibraryRepository: InMemoryExerciseLibraryRepository
    ): ExerciseLibraryRepository = inMemoryExerciseLibraryRepository
}
