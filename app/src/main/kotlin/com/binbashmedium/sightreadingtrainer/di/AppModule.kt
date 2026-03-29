package com.binbashmedium.sightreadingtrainer.di

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
}
