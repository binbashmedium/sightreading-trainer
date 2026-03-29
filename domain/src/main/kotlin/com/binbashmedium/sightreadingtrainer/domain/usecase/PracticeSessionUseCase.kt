package com.binbashmedium.sightreadingtrainer.domain.usecase

import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
import com.binbashmedium.sightreadingtrainer.domain.model.PracticeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PracticeSessionUseCase(
    private val matchNotesUseCase: MatchNotesUseCase = MatchNotesUseCase()
) {
    private val _state = MutableStateFlow<PracticeState?>(null)
    val state: StateFlow<PracticeState?> = _state.asStateFlow()

    fun startSession(exercise: Exercise) {
        _state.value = PracticeState(exercise = exercise)
    }

    fun processChord(playedNotes: List<NoteEvent>, toleranceMs: Long = 200L): MatchResult {
        val currentState = _state.value ?: return MatchResult.Waiting
        val exercise = currentState.exercise
        val expectedNotes = exercise.currentChord ?: return MatchResult.Waiting

        val result = matchNotesUseCase.execute(playedNotes, expectedNotes, toleranceMs)

        val newScore = if (result == MatchResult.Correct) currentState.score + 1 else currentState.score
        val newExercise = if (result == MatchResult.Correct) {
            exercise.copy(currentIndex = exercise.currentIndex + 1)
        } else {
            exercise
        }

        _state.value = currentState.copy(
            exercise = newExercise,
            lastResult = result,
            score = newScore,
            totalAttempts = currentState.totalAttempts + 1
        )

        return result
    }

    fun resetSession() {
        _state.value = null
    }
}
