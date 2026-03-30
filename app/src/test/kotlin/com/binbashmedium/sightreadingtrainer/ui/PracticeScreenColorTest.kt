package com.binbashmedium.sightreadingtrainer.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeScreenColorTest {

    @Test
    fun `colorForNoteState maps expected palette`() {
        assertEquals(Color.Black, colorForNoteState(NoteState.NONE))
        assertEquals(Color(0xFF2E7D32), colorForNoteState(NoteState.CORRECT))
        assertEquals(Color(0xFFC62828), colorForNoteState(NoteState.WRONG))
        assertEquals(Color(0xFFF9A825), colorForNoteState(NoteState.LATE))
    }

    @Test
    fun `stemColorForStates returns black when chord note states are mixed`() {
        val color = stemColorForStates(listOf(NoteState.CORRECT, NoteState.WRONG))
        assertEquals(Color.Black, color)
    }

    @Test
    fun `stemColorForStates follows state color when all notes match`() {
        val color = stemColorForStates(listOf(NoteState.CORRECT, NoteState.CORRECT))
        assertEquals(Color(0xFF2E7D32), color)
    }
}
