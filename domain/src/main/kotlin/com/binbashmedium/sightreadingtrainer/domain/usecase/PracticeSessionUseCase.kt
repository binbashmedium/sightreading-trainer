package com.binbashmedium.sightreadingtrainer.domain.usecase

import com.binbashmedium.sightreadingtrainer.core.util.NoteNames
import com.binbashmedium.sightreadingtrainer.domain.model.Exercise
import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import com.binbashmedium.sightreadingtrainer.domain.model.PerformanceInput
import com.binbashmedium.sightreadingtrainer.domain.model.PracticeState
import com.binbashmedium.sightreadingtrainer.domain.model.StepInputSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PracticeSessionUseCase(
    private val matchNotesUseCase: MatchNotesUseCase = MatchNotesUseCase()
) {
    companion object {
        private const val PEDAL_RELEASE_LEAD_TOLERANCE_MS = 1_000L
    }

    private val _state = MutableStateFlow<PracticeState?>(null)
    val state: StateFlow<PracticeState?> = _state.asStateFlow()
    private var pedalIsPressed: Boolean = false
    private var lastPedalReleaseTimestampMs: Long? = null

    fun startSession(exercise: Exercise, sessionDurationSec: Int = 60) {
        pedalIsPressed = false
        lastPedalReleaseTimestampMs = null
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
            resultByBeat = emptyMap(),
            inputByBeat = emptyMap()
        )
    }

    fun processInput(input: PerformanceInput, toleranceMs: Long = 200L): MatchResult {
        val currentState = _state.value ?: return MatchResult.Waiting
        val exercise = currentState.exercise
        val expectedStep = exercise.currentStep ?: return MatchResult.Waiting

        when (input.pedalAction) {
            PedalAction.PRESS -> pedalIsPressed = true
            PedalAction.RELEASE -> {
                pedalIsPressed = false
                lastPedalReleaseTimestampMs = input.timestamp
            }
            PedalAction.NONE -> Unit
        }

        if (input.notes.isEmpty()) {
            return MatchResult.Waiting
        }

        val inputSnapshot = StepInputSnapshot(
            playedNotes = input.notes.map { it.midiNote },
            playedPedalAction = input.pedalAction,
            inputTimestampMs = input.timestamp,
            pedalPressedAtInput = pedalIsPressed,
            lastPedalReleaseTimestampMs = lastPedalReleaseTimestampMs
        )
        val result = matchNotesUseCase.execute(
            playedNotes = input.notes,
            playedPedalAction = input.pedalAction,
            expectedStep = expectedStep,
            toleranceMs = toleranceMs,
            pedalIsPressed = pedalIsPressed,
            lastPedalReleaseTimestampMs = lastPedalReleaseTimestampMs,
            releaseLeadToleranceMs = PEDAL_RELEASE_LEAD_TOLERANCE_MS,
            playedInputTimestampMs = input.timestamp
        )
        val nowMs = System.currentTimeMillis()

        // Record result for this beat index (overwrites previous attempts on same chord).
        val newResultByBeat = currentState.resultByBeat + (exercise.currentIndex to result)
        val newInputByBeat = currentState.inputByBeat + (exercise.currentIndex to inputSnapshot)
        val noteOutcome = classifyNoteOutcome(expectedStep.notes, input.notes.map { it.midiNote })
        val stepGroup = stepGroupKey(expectedStep)

        val newCorrectGroupStats = if (result == MatchResult.Correct) {
            currentState.correctGroupStats.increment(stepGroup, 1)
        } else {
            currentState.correctGroupStats
        }
        val newWrongGroupStats = if (result == MatchResult.Correct) {
            currentState.wrongGroupStats
        } else {
            currentState.wrongGroupStats.increment(stepGroup, 1)
        }

        val newCorrectNoteStats = noteOutcome.correctMidi.fold(currentState.correctNoteStats) { acc, midi ->
            acc.increment(NoteNames.fromMidi(midi), 1)
        }
        val newWrongNoteStats = (noteOutcome.missingMidi + noteOutcome.extraMidi)
            .fold(currentState.wrongNoteStats) { acc, midi ->
                acc.increment(NoteNames.fromMidi(midi), 1)
            }

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
            inputByBeat = newInputByBeat,
            correctGroupStats = newCorrectGroupStats,
            wrongGroupStats = newWrongGroupStats,
            correctNoteStats = newCorrectNoteStats,
            wrongNoteStats = newWrongNoteStats,
            bpm = newBpm,
            lastCorrectTimestamp = newLastCorrectTs
        )

        return result
    }

    fun processChord(playedNotes: List<NoteEvent>, toleranceMs: Long = 200L): MatchResult =
        processInput(PerformanceInput(notes = playedNotes), toleranceMs)

    fun resetSession() {
        pedalIsPressed = false
        lastPedalReleaseTimestampMs = null
        _state.value = null
    }
}

private data class NoteOutcome(
    val correctMidi: List<Int>,
    val missingMidi: List<Int>,
    val extraMidi: List<Int>
)

private fun classifyNoteOutcome(expected: List<Int>, played: List<Int>): NoteOutcome {
    val playedCounts = played.groupingBy { it }.eachCount().toMutableMap()
    val correct = mutableListOf<Int>()
    val missing = mutableListOf<Int>()
    expected.forEach { midi ->
        val count = playedCounts[midi] ?: 0
        if (count > 0) {
            correct += midi
            playedCounts[midi] = count - 1
        } else {
            missing += midi
        }
    }
    val extra = buildList {
        playedCounts.forEach { (midi, count) ->
            repeat(count.coerceAtLeast(0)) { add(midi) }
        }
    }
    return NoteOutcome(correctMidi = correct, missingMidi = missing, extraMidi = extra)
}

private fun stepGroupKey(step: com.binbashmedium.sightreadingtrainer.domain.model.ExerciseStep): String {
    step.contentType?.let { return it.name }
    return when (step.notes.size) {
        1 -> "SINGLE_NOTES"
        2 -> {
            val interval = kotlin.math.abs(step.notes[1] - step.notes[0])
            when (interval) {
                12 -> "OCTAVES"
                3, 4 -> "THIRDS"
                7 -> "FIFTHS"
                8, 9 -> "SIXTHS"
                else -> "INTERVALS"
            }
        }
        3 -> "TRIADS"
        4 -> "SEVENTHS"
        5 -> "NINTHS"
        else -> "CHORDS"
    }
}

private fun Map<String, Int>.increment(key: String, delta: Int): Map<String, Int> =
    this + (key to ((this[key] ?: 0) + delta))
