package com.binbashmedium.sightreadingtrainer.ui

import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import kotlin.math.absoluteValue

val KEY_NAMES = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

/**
 * Key signatures as (sharps, flats) pairs indexed by musicalKey (0=C … 11=B).
 * C# uses 7 sharps; enharmonic spellings with flats follow standard conventions.
 */
val KEY_SIGNATURES: Array<Pair<Int, Int>> = arrayOf(
    0 to 0,  // C
    7 to 0,  // C#
    2 to 0,  // D
    0 to 3,  // Eb
    4 to 0,  // E
    0 to 1,  // F
    6 to 0,  // F#
    1 to 0,  // G
    0 to 4,  // Ab
    3 to 0,  // A
    0 to 2,  // Bb
    5 to 0   // B
)

// Accidental placement: diatonic steps for each sharp/flat in order, per staff.
// Treble: E4=bottom line at diatonic step 30; each step = lineSpacing/2.
val TREBLE_SHARP_STEPS = intArrayOf(38, 35, 39, 36, 33, 37, 34) // F5,C5,G5,D5,A4,E5,B4
val TREBLE_FLAT_STEPS  = intArrayOf(34, 37, 33, 36, 32, 35, 31) // B4,E5,A4,D5,G4,C5,F4
// Bass: G2=bottom line at diatonic step 18.
val BASS_SHARP_STEPS   = intArrayOf(24, 21, 25, 22, 19, 23, 20) // F3,C3,G3,D3,A2,E3,B2
val BASS_FLAT_STEPS    = intArrayOf(20, 23, 19, 22, 18, 21, 17) // B2,E3,A2,D3,G2,C3,F2

const val TREBLE_BOTTOM_LINE_STEP = 30
const val TREBLE_G_LINE_STEP = 32
const val TREBLE_TOP_LINE_STEP = 38
const val BASS_BOTTOM_LINE_STEP = 18
const val BASS_F_LINE_STEP = 24
const val BASS_TOP_LINE_STEP = 26

enum class NoteState {
    NONE,
    CORRECT,
    WRONG,
    LATE
}

enum class StaffType { TREBLE, BASS }

data class NoteEvent(
    val midi: Int,
    val startBeat: Float,
    val duration: Float,
    val expected: Boolean,
    val state: NoteState = NoteState.NONE,
    val staff: StaffType = StaffType.TREBLE
)

data class Chord(
    val name: String,
    val notes: List<Int>,
    val startBeat: Float
)

data class GameState(
    val levelTitle: String,
    val elapsedTime: Long,
    val score: Int,
    val bpm: Float,
    val notes: List<NoteEvent>,
    val chords: List<Chord>,
    val currentBeat: Float,
    val musicalKey: Int = 0
)

fun formatElapsedTime(elapsedTimeMs: Long): String {
    val totalSeconds = (elapsedTimeMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Returns the diatonic step number for a MIDI note.
 * C-1 (MIDI 0) = step 0; C4 (MIDI 60) = step 28; E4 (MIDI 64) = step 30.
 *
 * Mapping: C=0, D=1, E=2, F=3, G=4, A=5, B=6 within each octave.
 */
fun midiToDiatonicStep(midi: Int): Int {
    val noteClassToStep = intArrayOf(0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6)
    val noteIdx = noteClassToStep[midi % 12]
    val octave = (midi / 12) - 1  // MIDI 60 = C4 → octave 4
    return octave * 7 + noteIdx
}

/**
 * Converts a MIDI number to a canvas Y coordinate on the grand staff.
 *
 * Treble reference: E4 (diatonic step 30) = bottom line = trebleTopY + 4*lineSpacing.
 * Bass reference:   G2 (diatonic step 18) = bottom line = bassTopY  + 4*lineSpacing.
 * Each diatonic step = lineSpacing/2 vertically.
 */
fun midiToGrandStaffY(
    midi: Int,
    staff: StaffType,
    trebleTopY: Float,
    bassTopY: Float,
    lineSpacing: Float
): Float = staffLineYForStep(midiToDiatonicStep(midi), staff, trebleTopY, bassTopY, lineSpacing)

fun staffLineYForStep(
    diatonicStep: Int,
    staff: StaffType,
    trebleTopY: Float,
    bassTopY: Float,
    lineSpacing: Float
): Float = when (staff) {
    StaffType.TREBLE -> (trebleTopY + 4f * lineSpacing) - (diatonicStep - TREBLE_BOTTOM_LINE_STEP) * lineSpacing / 2f
    StaffType.BASS -> (bassTopY + 4f * lineSpacing) - (diatonicStep - BASS_BOTTOM_LINE_STEP) * lineSpacing / 2f
}

fun staffForExercise(midi: Int, handMode: HandMode): StaffType = when (handMode) {
    HandMode.LEFT -> StaffType.BASS
    HandMode.RIGHT -> StaffType.TREBLE
    HandMode.BOTH -> if (midi >= 60) StaffType.TREBLE else StaffType.BASS
}

/**
 * Returns the diatonic steps at which ledger lines should be drawn below the staff.
 * staffBottomStep: diatonic step of the lowest staff line.
 */
fun ledgerStepsBelow(noteDiatonicStep: Int, staffBottomStep: Int): List<Int> {
    val lines = mutableListOf<Int>()
    var l = staffBottomStep - 2
    while (l >= noteDiatonicStep) {
        lines.add(l)
        l -= 2
    }
    return lines
}

/**
 * Returns the diatonic steps at which ledger lines should be drawn above the staff.
 * staffTopStep: diatonic step of the highest staff line.
 */
fun ledgerStepsAbove(noteDiatonicStep: Int, staffTopStep: Int): List<Int> {
    val lines = mutableListOf<Int>()
    var l = staffTopStep + 2
    while (l <= noteDiatonicStep) {
        lines.add(l)
        l += 2
    }
    return lines
}

fun beatToX(
    beat: Float,
    startX: Float,
    beatWidth: Float
): Float = startX + (beat * beatWidth)

fun durationToGlyphType(duration: Float): NoteGlyphType {
    val normalized = duration.absoluteValue
    return when {
        normalized >= 4f   -> NoteGlyphType.WHOLE
        normalized >= 2f   -> NoteGlyphType.HALF
        normalized >= 1f   -> NoteGlyphType.QUARTER
        normalized >= 0.5f -> NoteGlyphType.EIGHTH
        else               -> NoteGlyphType.SIXTEENTH
    }
}

enum class NoteGlyphType {
    WHOLE,
    HALF,
    QUARTER,
    EIGHTH,
    SIXTEENTH
}
