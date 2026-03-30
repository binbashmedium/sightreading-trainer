package com.binbashmedium.sightreadingtrainer.domain.usecase

import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
import com.binbashmedium.sightreadingtrainer.domain.model.PerformanceInput
import com.binbashmedium.sightreadingtrainer.domain.model.PracticeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PracticeSessionUseCase(
    private val matchNotesUseCase: MatchNotesUseCase = MatchNotesUseCase()
) {
    private val _state = MutableStateFlow<PracticeState?>(null)
    val state: StateFlow<PracticeState?> = _state.asStateFlow()

    fun startSession(exercise: Exercise, sessionDurationSec: Int = 60) {
        _state.value = PracticeState(
            exercise = exercise,
            sessionDurationSec = sessionDurationSec.coerceAtLeast(10)
        )
    }

    fun loadNextExercise(exercise: Exercise) {
        val currentState = _state.value ?: return
        _state.value = currentState.copy(
            exercise = exercise.copy(currentIndex = 0),
            lastResult = MatchResult.Waiting,
            resultByBeat = emptyMap()
        )
    }

    fun processInput(input: PerformanceInput, toleranceMs: Long = 200L): MatchResult {
        val currentState = _state.value ?: return MatchResult.Waiting
        val exercise = currentState.exercise
        val expectedStep = exercise.currentStep ?: return MatchResult.Waiting

        val result = matchNotesUseCase.execute(input.notes, input.pedalAction, expectedStep, toleranceMs)
        val nowMs = System.currentTimeMillis()

        // Record result for this beat index (overwrites previous attempts on same chord).
        val newResultByBeat = currentState.resultByBeat + (exercise.currentIndex to result)

        val (newScore, newBpm, newLastCorrectTs) = if (result == MatchResult.Correct) {
            val interChordMs = if (currentState.lastCorrectTimestamp > 0L)
                nowMs - currentState.lastCorrectTimestamp
            else
                Long.MAX_VALUE

            // Fluency bonus: up to +10 pts for fast playing (< 2 s inter-chord).
            val fluencyBonus = if (interChordMs < Long.MAX_VALUE)
                maxOf(0L, (2000L - interChordMs) / 200L).toInt()
            else
                0

            // BPM from the last inter-chord interval, capped at 300.
            val updatedBpm = if (interChordMs < Long.MAX_VALUE)
                (60_000f / interChordMs.coerceAtLeast(1L)).coerceIn(0f, 300f)
            else
                currentState.bpm

            Triple(currentState.score + 10 + fluencyBonus, updatedBpm, nowMs)
        } else {
            Triple(currentState.score, currentState.bpm, currentState.lastCorrectTimestamp)
        }

        val expectedNotesCount = expectedStep.notes.size
        val (newCorrectNotes, newWrongNotes) = when (result) {
            MatchResult.Correct -> (currentState.correctNotesCount + expectedNotesCount) to currentState.wrongNotesCount
            MatchResult.Incorrect, MatchResult.TooEarly, MatchResult.TooLate ->
                currentState.correctNotesCount to (currentState.wrongNotesCount + expectedNotesCount)
            MatchResult.Waiting -> currentState.correctNotesCount to currentState.wrongNotesCount
        }

        // Always advance to the next step regardless of whether the result is correct or not.
        val newExercise = exercise.copy(currentIndex = exercise.currentIndex + 1)

        _state.value = currentState.copy(
            exercise = newExercise,
            lastResult = result,
            score = newScore,
            totalAttempts = currentState.totalAttempts + 1,
            correctNotesCount = newCorrectNotes,
            wrongNotesCount = newWrongNotes,
            resultByBeat = newResultByBeat,
            bpm = newBpm,
            lastCorrectTimestamp = newLastCorrectTs
        )

        return result
    }

    fun processChord(playedNotes: List<NoteEvent>, toleranceMs: Long = 200L): MatchResult =
        processInput(PerformanceInput(notes = playedNotes), toleranceMs)

    fun resetSession() {
        _state.value = null
    }
}
