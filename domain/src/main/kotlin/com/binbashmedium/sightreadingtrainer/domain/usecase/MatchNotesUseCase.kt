package com.binbashmedium.sightreadingtrainer.domain.usecase

import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent

class MatchNotesUseCase {

    fun execute(
        playedNotes: List<NoteEvent>,
        expectedNotes: List<Int>,
        toleranceMs: Long = 200L,
        expectedTimeMs: Long? = null
    ): MatchResult {
        val playedMidiNotes = playedNotes.map { it.midiNote }.sorted()
        val sortedExpected = expectedNotes.sorted()

        if (playedMidiNotes != sortedExpected) {
            return MatchResult.Incorrect
        }

        if (expectedTimeMs != null) {
            val playedTime = playedNotes.firstOrNull()?.timestamp ?: return MatchResult.Correct
            val delta = playedTime - expectedTimeMs
            return when {
                delta < -toleranceMs -> MatchResult.TooEarly
                delta > toleranceMs -> MatchResult.TooLate
                else -> MatchResult.Correct
            }
        }

        return MatchResult.Correct
    }
}
