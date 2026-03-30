package com.binbashmedium.sightreadingtrainer.ui

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsViewModelTest {

    @Test
    fun `toTop5 keeps only highest five entries`() {
        val stats = mapOf(
            "A" to 1,
            "B" to 8,
            "C" to 3,
            "D" to 10,
            "E" to 7,
            "F" to 2
        )

        val top = stats.toTop5(prettyGroupNames = false)

        assertEquals(5, top.size)
        assertEquals("D", top[0].label)
        assertEquals(10, top[0].count)
        assertTrue(top.none { it.label == "A" })
    }

    @Test
    fun `toStatisticsUiState maps group names with spaces`() {
        val settings = AppSettings(
            correctGroupStats = mapOf("SINGLE_NOTES" to 12),
            wrongGroupStats = mapOf("TRIADS" to 5),
            correctNoteStats = mapOf("C4" to 9),
            wrongNoteStats = mapOf("F#4" to 4)
        )

        val state = settings.toStatisticsUiState()

        assertEquals("SINGLE NOTES", state.topCorrectGroups.first().label)
        assertEquals(12, state.topCorrectGroups.first().count)
        assertEquals("TRIADS", state.topWrongGroups.first().label)
        assertEquals("C4", state.topCorrectNotes.first().label)
        assertEquals("F#4", state.topWrongNotes.first().label)
    }
}
