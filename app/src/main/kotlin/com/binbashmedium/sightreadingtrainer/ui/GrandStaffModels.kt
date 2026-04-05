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

package com.binbashmedium.sightreadingtrainer.ui

import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteAccidental
import com.binbashmedium.sightreadingtrainer.domain.model.NoteValue
import com.binbashmedium.sightreadingtrainer.domain.model.OrnamentType
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import com.binbashmedium.sightreadingtrainer.domain.model.StepInputSnapshot
import kotlin.math.absoluteValue

val KEY_NAMES = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

/**
 * Key signatures as (sharps, flats) pairs indexed by musicalKey (0=C ... 11=B).
 * C# uses 7 sharps; enharmonic spellings with flats follow standard conventions.
 */
val KEY_SIGNATURES: Array<Pair<Int, Int>> = arrayOf(
    0 to 0,
    7 to 0,
    2 to 0,
    0 to 3,
    4 to 0,
    0 to 1,
    6 to 0,
    1 to 0,
    0 to 4,
    3 to 0,
    0 to 2,
    5 to 0
)

val TREBLE_SHARP_STEPS = intArrayOf(38, 35, 39, 36, 33, 37, 34)
val TREBLE_FLAT_STEPS = intArrayOf(34, 37, 33, 36, 32, 35, 31)
val BASS_SHARP_STEPS = intArrayOf(24, 21, 25, 22, 19, 23, 20)
val BASS_FLAT_STEPS = intArrayOf(20, 23, 19, 22, 18, 21, 17)

const val TREBLE_BOTTOM_LINE_STEP = 30
const val TREBLE_G_LINE_STEP = 32
const val TREBLE_MIDDLE_LINE_STEP = 34
const val TREBLE_TOP_LINE_STEP = 38
const val BASS_BOTTOM_LINE_STEP = 18
const val BASS_F_LINE_STEP = 24
const val BASS_MIDDLE_LINE_STEP = 22
const val BASS_TOP_LINE_STEP = 26
const val BASS_CLEF_LOWER_DOT_STEP = 23
const val BASS_CLEF_UPPER_DOT_STEP = 25
const val TREBLE_CLEF_BASELINE_OFFSET = 0.95f
const val BASS_CLEF_BASELINE_OFFSET = 2.68f
const val CLEF_AREA_WIDTH_RATIO = 3.8f
const val POST_CLEF_GAP_RATIO = 2.15f
const val ACCIDENTAL_SPACING_RATIO = 0.75f
const val ACCIDENTAL_TEXT_SIZE_RATIO = 2.2f
const val TREBLE_CLEF_TEXT_SIZE_RATIO = 4.0f
const val BASS_CLEF_TEXT_SIZE_RATIO = 4.0f
const val BASS_CLEF_BASELINE_FROM_TOP_RATIO = 0.92f
const val BASS_CLEF_DOT_RADIUS_RATIO = 0.17f
const val BASS_CLEF_DOT_X_OFFSET_RATIO = 1.02f
const val BASS_CLEF_GLYPH_X_OFFSET_RATIO = 0.22f
const val KEY_SIGNATURE_LEAD_IN_RATIO = 0.72f
const val KEY_SIGNATURE_X_OFFSET_RATIO = 0.38f
const val NOTE_ACCIDENTAL_TEXT_SIZE_RATIO = 1.08f
/**
 * Bar lines are shifted left by this fraction of lineSpacing.
 * 1.5 provides ≥ one notehead-width of clear space on both sides of every bar line:
 *   • After bar line → first note of next measure: gap ≈ 1.5 × lineSpacing − 0.55 × lineSpacing ≈ noteheadWidth.
 *   • Before bar line → last note of current measure (quarter at beat 6): gap ≈ 2 × beatWidth − 1.5 × lineSpacing.
 * The generation constraint (BARLINE_GAP_BEATS) ensures the last notehead is always ≥ 2 UI-beat-units
 * (= 1 quarter note) before the bar, making the before-bar gap visually clear at typical screen sizes.
 */
const val BAR_LINE_SHIFT_RATIO = 1.5f

// ── Beat / layout constants ──────────────────────────────────────────────────
/** UI beat-units per quarter note. WHOLE=8, HALF=4, QUARTER=2, EIGHTH=1. */
const val BEATS_PER_STEP = 2f
/** Measures shown per grand-staff row (portrait and landscape). */
const val MEASURES_PER_ROW = 4
/** Grand-staff rows shown per page in portrait mode. */
const val ROWS_PER_PAGE = 4
/** Beat-units per measure = 4 quarter notes × BEATS_PER_STEP = 8f. */
const val BEATS_PER_MEASURE_UNITS = 4 * BEATS_PER_STEP                   // 8f
/** Beat-units per row = MEASURES_PER_ROW × BEATS_PER_MEASURE_UNITS = 32f. */
const val BEATS_PER_ROW = MEASURES_PER_ROW * BEATS_PER_MEASURE_UNITS     // 32f
/** Beat-units per page = ROWS_PER_PAGE × BEATS_PER_ROW = 128f. */
const val BEATS_PER_PAGE = ROWS_PER_PAGE * BEATS_PER_ROW                 // 128f

/** Extension: converts a [NoteValue] to UI beat-units for positioning. */
val NoteValue.uiBeatUnits: Float get() = beats * BEATS_PER_STEP
// WHOLE=8f, HALF=4f, QUARTER=2f, EIGHTH=1f

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
    val staff: StaffType = StaffType.TREBLE,
    val accidental: NoteAccidental = NoteAccidental.NONE,
    /** Ornament on this note (only the first note of a step carries it). */
    val ornament: OrnamentType = OrnamentType.NONE
)

data class PedalMark(
    val startBeat: Float,
    val action: PedalAction,
    val state: NoteState = NoteState.NONE
)

data class Chord(
    val name: String,
    val notes: List<Int>,
    val startBeat: Float,
    /** Which staff the label should appear on. TREBLE → above treble staff; BASS → below bass staff. */
    val staff: StaffType = StaffType.TREBLE
)

data class GameState(
    val levelTitle: String,
    val elapsedTime: Long,
    val bpm: Float,
    val notes: List<NoteEvent>,
    val chords: List<Chord>,
    val pedalMarks: List<PedalMark>,
    val currentBeat: Float,
    val musicalKey: Int = 0
)

data class GrandStaffLayoutMetrics(
    val clefAreaWidth: Float,
    val postClefGap: Float,
    val accidentalSpacing: Float,
    val accidentalTextSize: Float,
    val trebleClefTextSize: Float,
    val keySignatureWidth: Float,
    val keySignatureStartOffset: Float
)

data class BassClefGeometry(
    val glyphX: Float,
    val baselineY: Float,
    val textSize: Float,
    val topY: Float,
    val dotCenterX: Float,
    val upperDotY: Float,
    val lowerDotY: Float,
    val dotRadius: Float
)

enum class StemDirection { UP, DOWN }

data class StemGeometry(
    val x: Float,
    val startY: Float,
    val endY: Float
)

data class StepNoteDisplayOutcome(
    val expectedStates: List<NoteState>,
    val extraNotes: List<Int>
)

fun formatElapsedTime(elapsedTimeMs: Long): String {
    val totalSeconds = (elapsedTimeMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

fun midiToDiatonicStep(midi: Int): Int {
    val noteClassToStep = intArrayOf(0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6)
    val noteIdx = noteClassToStep[midi % 12]
    val octave = (midi / 12) - 1
    return octave * 7 + noteIdx
}

fun displayDiatonicStep(midi: Int, accidental: NoteAccidental = NoteAccidental.NONE): Int =
    midiToDiatonicStep(midi) + if (accidental == NoteAccidental.FLAT) 1 else 0

fun midiToGrandStaffY(
    midi: Int,
    staff: StaffType,
    trebleTopY: Float,
    bassTopY: Float,
    lineSpacing: Float,
    accidental: NoteAccidental = NoteAccidental.NONE
): Float = staffLineYForStep(displayDiatonicStep(midi, accidental), staff, trebleTopY, bassTopY, lineSpacing)

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

fun trebleClefBaselineY(anchorY: Float, lineSpacing: Float): Float =
    anchorY + lineSpacing * TREBLE_CLEF_BASELINE_OFFSET

fun bassClefBaselineY(anchorY: Float, lineSpacing: Float): Float =
    anchorY + lineSpacing * BASS_CLEF_BASELINE_OFFSET

fun middleLineStepForStaff(staff: StaffType): Int = when (staff) {
    StaffType.TREBLE -> TREBLE_MIDDLE_LINE_STEP
    StaffType.BASS -> BASS_MIDDLE_LINE_STEP
}

fun grandStaffLayoutMetrics(lineSpacing: Float, numAccidentals: Int): GrandStaffLayoutMetrics {
    val accidentalSpacing = lineSpacing * ACCIDENTAL_SPACING_RATIO
    return GrandStaffLayoutMetrics(
        clefAreaWidth = lineSpacing * CLEF_AREA_WIDTH_RATIO,
        postClefGap = lineSpacing * POST_CLEF_GAP_RATIO,
        accidentalSpacing = accidentalSpacing,
        accidentalTextSize = lineSpacing * ACCIDENTAL_TEXT_SIZE_RATIO,
        trebleClefTextSize = lineSpacing * TREBLE_CLEF_TEXT_SIZE_RATIO,
        keySignatureWidth = keySignatureWidth(numAccidentals, lineSpacing),
        keySignatureStartOffset = lineSpacing * KEY_SIGNATURE_X_OFFSET_RATIO
    )
}

fun keySignatureWidth(numAccidentals: Int, lineSpacing: Float): Float {
    if (numAccidentals <= 0) return 0f
    val accidentalSpacing = lineSpacing * ACCIDENTAL_SPACING_RATIO
    return (numAccidentals - 1) * accidentalSpacing + lineSpacing * KEY_SIGNATURE_LEAD_IN_RATIO
}

fun noteAccidentalSymbol(accidental: NoteAccidental): String? = when (accidental) {
    NoteAccidental.SHARP -> "♯"
    NoteAccidental.FLAT -> "♭"
    NoteAccidental.NATURAL -> "♮"
    NoteAccidental.NONE -> null
}

fun classifyStepNotes(expectedNotes: List<Int>, playedNotes: List<Int>): StepNoteDisplayOutcome {
    val playedCounts = playedNotes.groupingBy { it }.eachCount().toMutableMap()
    val expectedStates = expectedNotes.map { expectedMidi ->
        val count = playedCounts[expectedMidi] ?: 0
        if (count > 0) {
            playedCounts[expectedMidi] = count - 1
            NoteState.CORRECT
        } else {
            NoteState.WRONG
        }
    }
    val extras = buildList {
        playedCounts.forEach { (midi, count) ->
            repeat(count.coerceAtLeast(0)) { add(midi) }
        }
    }
    return StepNoteDisplayOutcome(expectedStates = expectedStates, extraNotes = extras)
}

fun isExpectedPedalSatisfied(
    expectedAction: PedalAction,
    snapshot: StepInputSnapshot,
    releaseLeadToleranceMs: Long = 1_000L
): Boolean = when (expectedAction) {
    PedalAction.NONE -> true
    PedalAction.PRESS -> snapshot.playedPedalAction == PedalAction.PRESS || snapshot.pedalPressedAtInput
    PedalAction.RELEASE -> {
        val releasedOnThisInput = snapshot.playedPedalAction == PedalAction.RELEASE
        val releaseTs = snapshot.lastPedalReleaseTimestampMs
        val recentRelease = releaseTs != null &&
            snapshot.inputTimestampMs >= releaseTs &&
            snapshot.inputTimestampMs - releaseTs <= releaseLeadToleranceMs
        releasedOnThisInput || recentRelease
    }
}

fun accidentalForPlayedMidi(midi: Int): NoteAccidental = when ((midi % 12 + 12) % 12) {
    1, 3, 6, 8, 10 -> NoteAccidental.SHARP
    else -> NoteAccidental.NONE
}

fun pedalMarkText(action: PedalAction): String? = when (action) {
    PedalAction.PRESS -> "Ped."
    PedalAction.RELEASE -> "✱"
    PedalAction.NONE -> null
}

fun accidentalColumnsForSteps(stepsAscending: List<Int>): List<Int> {
    if (stepsAscending.isEmpty()) return emptyList()
    val columns = MutableList(stepsAscending.size) { 0 }
    for (index in 1 until stepsAscending.size) {
        columns[index] = if (stepsAscending[index] - stepsAscending[index - 1] < 4) {
            columns[index - 1] + 1
        } else {
            0
        }
    }
    return columns
}

fun bassClefGeometry(
    clefX: Float,
    trebleTopY: Float,
    bassTopY: Float,
    lineSpacing: Float
): BassClefGeometry {
    val topY = staffLineYForStep(BASS_TOP_LINE_STEP, StaffType.BASS, trebleTopY, bassTopY, lineSpacing)
    val lowerDotY = staffLineYForStep(BASS_CLEF_LOWER_DOT_STEP, StaffType.BASS, trebleTopY, bassTopY, lineSpacing)
    val upperDotY = staffLineYForStep(BASS_CLEF_UPPER_DOT_STEP, StaffType.BASS, trebleTopY, bassTopY, lineSpacing)
    val textSize = lineSpacing * BASS_CLEF_TEXT_SIZE_RATIO
    return BassClefGeometry(
        glyphX = clefX + lineSpacing * BASS_CLEF_GLYPH_X_OFFSET_RATIO,
        baselineY = topY + textSize * BASS_CLEF_BASELINE_FROM_TOP_RATIO,
        textSize = textSize,
        topY = topY,
        dotCenterX = clefX + lineSpacing * BASS_CLEF_DOT_X_OFFSET_RATIO,
        upperDotY = upperDotY,
        lowerDotY = lowerDotY,
        dotRadius = lineSpacing * BASS_CLEF_DOT_RADIUS_RATIO
    )
}

fun ledgerStepsBelow(noteDiatonicStep: Int, staffBottomStep: Int): List<Int> {
    val lines = mutableListOf<Int>()
    var l = staffBottomStep - 2
    while (l >= noteDiatonicStep) {
        lines.add(l)
        l -= 2
    }
    return lines
}

fun ledgerStepsAbove(noteDiatonicStep: Int, staffTopStep: Int): List<Int> {
    val lines = mutableListOf<Int>()
    var l = staffTopStep + 2
    while (l <= noteDiatonicStep) {
        lines.add(l)
        l += 2
    }
    return lines
}

fun stemDirectionForSteps(steps: List<Int>, staff: StaffType): StemDirection {
    if (steps.isEmpty()) return StemDirection.UP
    val middle = middleLineStepForStaff(staff)
    val highestDistance = (steps.maxOrNull() ?: middle) - middle
    val lowestDistance = middle - (steps.minOrNull() ?: middle)
    return when {
        highestDistance > lowestDistance -> StemDirection.DOWN
        lowestDistance > highestDistance -> StemDirection.UP
        else -> StemDirection.DOWN
    }
}

fun chordNoteheadOffsets(
    stepsAscending: List<Int>,
    direction: StemDirection,
    lineSpacing: Float
): List<Float> {
    if (stepsAscending.isEmpty()) return emptyList()
    val offsets = MutableList(stepsAscending.size) { 0f }
    // Displacement equals one notehead width so the displaced notehead clears the stem
    val displacement = lineSpacing * 1.1f
    for (index in 1 until stepsAscending.size) {
        if (stepsAscending[index] - stepsAscending[index - 1] == 1) {
            when (direction) {
                StemDirection.UP -> offsets[index] = displacement
                StemDirection.DOWN -> offsets[index - 1] = -displacement
            }
        }
    }
    return offsets
}

fun stemGeometryForChord(
    noteXs: List<Float>,
    noteYs: List<Float>,
    direction: StemDirection,
    middleLineY: Float,
    lineSpacing: Float,
    noteHeadWidth: Float
): StemGeometry {
    val defaultStemLength = lineSpacing * 3.5f
    return when (direction) {
        StemDirection.UP -> {
            // Non-displaced noteheads sit at the minimum x; stem attaches to their right edge.
            // Displaced noteheads (seconds) are to the right of the stem — do not use them for x.
            val stemNoteX = noteXs.minOrNull() ?: 0f
            val startY = noteYs.maxOrNull() ?: middleLineY
            val referenceY = noteYs.minOrNull() ?: startY
            StemGeometry(
                x = stemNoteX + noteHeadWidth * 0.5f,
                startY = startY,
                endY = minOf(referenceY - defaultStemLength, middleLineY)
            )
        }
        StemDirection.DOWN -> {
            // Non-displaced noteheads sit at the maximum x; stem attaches to their left edge.
            // Displaced noteheads (seconds) are to the left of the stem — do not use them for x.
            val stemNoteX = noteXs.maxOrNull() ?: 0f
            val startY = noteYs.minOrNull() ?: middleLineY
            val referenceY = noteYs.maxOrNull() ?: startY
            StemGeometry(
                x = stemNoteX - noteHeadWidth * 0.5f,
                startY = startY,
                endY = maxOf(referenceY + defaultStemLength, middleLineY)
            )
        }
    }
}

fun beatToX(
    beat: Float,
    startX: Float,
    beatWidth: Float
): Float = startX + (beat * beatWidth)

fun durationToGlyphType(duration: Float): NoteGlyphType {
    val normalized = duration.absoluteValue
    return when {
        normalized >= 4f -> NoteGlyphType.WHOLE
        normalized >= 2f -> NoteGlyphType.HALF
        normalized >= 1f -> NoteGlyphType.QUARTER
        normalized >= 0.5f -> NoteGlyphType.EIGHTH
        else -> NoteGlyphType.SIXTEENTH
    }
}

enum class NoteGlyphType {
    WHOLE,
    HALF,
    QUARTER,
    EIGHTH,
    SIXTEENTH
}

/**
 * A group of consecutive eighth notes on the same staff and quarter-beat that are
 * connected by a single beam bar instead of individual flags.
 *
 * @param beatPositions All distinct beat positions in this group, sorted ascending.
 * @param staff The staff this beam group belongs to.
 * @param direction Unified stem direction for all notes in this group.
 */
data class BeamGroup(
    val beatPositions: List<Float>,
    val staff: StaffType,
    val direction: StemDirection
)

/**
 * Groups consecutive eighth notes in [notes] into beam groups per the 4/4 metric rule:
 * beam per quarter beat (groups of 2 eighths per beat). Only groups of ≥2 distinct beat
 * positions are returned.
 *
 * Stem direction is governed by the note farthest from the middle line across the whole group.
 */
fun computeBeamGroups(notes: List<NoteEvent>): List<BeamGroup> {
    val eighths = notes.filter { it.duration == 0.5f }
    if (eighths.isEmpty()) return emptyList()
    // Quarter-beat index: two UI beat-units = one quarter beat (BEATS_PER_STEP = 2f)
    val grouped = eighths.groupBy { note ->
        note.staff to (note.startBeat / BEATS_PER_STEP).toInt()
    }
    return grouped.mapNotNull { (key, groupNotes) ->
        val (staff, _) = key
        val distinctBeats = groupNotes.map { it.startBeat }.distinct().sorted()
        if (distinctBeats.size < 2) return@mapNotNull null
        val allSteps = groupNotes.map { displayDiatonicStep(it.midi, it.accidental) }
        val direction = stemDirectionForSteps(allSteps, staff)
        BeamGroup(beatPositions = distinctBeats, staff = staff, direction = direction)
    }
}

private val SCALE_DEGREES = listOf(0, 2, 4, 5, 7, 9, 11)
private val ROMAN_NUMERALS = listOf("I", "ii", "iii", "IV", "V", "vi", "vii")
private val CHORD_QUALITIES = mapOf(
    listOf(0, 4, 7) to "M",
    listOf(0, 3, 7) to "m",
    listOf(0, 3, 6) to "dim",
    listOf(0, 4, 7, 11) to "M7",
    listOf(0, 4, 7, 10) to "7",
    listOf(0, 3, 7, 10) to "m7",
    listOf(0, 3, 6, 10) to "m7b5",
    listOf(0, 2, 4, 7, 11) to "M9",
    listOf(0, 2, 4, 7, 10) to "9",
    listOf(0, 2, 3, 7, 10) to "m9"
)

private data class DetectedChord(
    val rootPitchClass: Int,
    val quality: String
)

fun formatChordLabel(notes: List<Int>, musicalKey: Int): String {
    if (notes.isEmpty()) return "?"
    if (notes.size == 1) return noteName(notes.first())
    if (notes.size == 2) return notes.joinToString(" - ") { noteName(it) }

    val detected = detectChord(notes)
    if (detected != null) {
        val rootName = KEY_NAMES[detected.rootPitchClass]
        val roman = romanNumeralForChord(detected.rootPitchClass, detected.quality, musicalKey)
        return if (roman != null) "$rootName${detected.quality} ($roman)" else "$rootName${detected.quality}"
    }

    return notes.joinToString(" - ") { noteName(it) }
}

/**
 * Compact chord label without the Roman-numeral suffix.
 * Used when horizontal space is tight (label area ≈ beat width).
 * Single notes → note name, intervals → "X-Y", chords → "RootQuality".
 */
fun formatChordLabelShort(notes: List<Int>): String {
    if (notes.isEmpty()) return "?"
    if (notes.size == 1) return noteName(notes.first())
    if (notes.size == 2) return "${noteName(notes.first())}-${noteName(notes.last())}"
    val detected = detectChord(notes)
    if (detected != null) return "${KEY_NAMES[detected.rootPitchClass]}${detected.quality}"
    return notes.take(2).joinToString("-") { noteName(it) }
}

fun chordQualitySuffix(notes: List<Int>): String = detectChord(notes)?.quality.orEmpty()

fun romanNumeralForChord(rootPitchClass: Int, quality: String, musicalKey: Int): String? {
    val degreeOffset = (rootPitchClass - musicalKey + 12) % 12
    val degreeIndex = SCALE_DEGREES.indexOf(degreeOffset)
    if (degreeIndex == -1) return null

    val baseRoman = when (quality) {
        "dim" -> "vii°"
        "m7b5" -> "viiø7"
        else -> ROMAN_NUMERALS[degreeIndex]
    }

    return when (quality) {
        "M7" -> "${baseRoman}maj7"
        "7", "m7" -> "${baseRoman}7"
        "M9" -> "${baseRoman}maj9"
        "9", "m9" -> "${baseRoman}9"
        else -> baseRoman
    }
}

private fun detectChord(notes: List<Int>): DetectedChord? {
    val pitchClasses = notes
        .map { ((it % 12) + 12) % 12 }
        .distinct()
        .sorted()

    for (candidateRoot in pitchClasses) {
        val intervals = pitchClasses
            .map { (it - candidateRoot + 12) % 12 }
            .distinct()
            .sorted()
        val quality = CHORD_QUALITIES[intervals] ?: continue
        return DetectedChord(candidateRoot, quality)
    }

    return null
}

/** Returns the 0-based page index for the given beat (portrait pagination). */
fun beatToPage(beat: Float): Int = (beat / BEATS_PER_PAGE).toInt()

/** Returns the start beat of the given 0-based page. */
fun pageStartBeat(page: Int): Float = page * BEATS_PER_PAGE

/** Returns the 1-based measure number for the first measure in the given portrait row. */
fun rowMeasureLabel(rowStartBeat: Float): Int = (rowStartBeat / BEATS_PER_MEASURE_UNITS).toInt() + 1

fun noteName(midi: Int): String {
    val pitchClass = ((midi % 12) + 12) % 12
    val octave = (midi / 12) - 1
    return "${KEY_NAMES[pitchClass]}$octave"
}

fun generateExampleGameState(nowMs: Long = System.currentTimeMillis()): GameState {
    val phase = ((nowMs / 1_000L) % 16L).toInt()
    val seed = (nowMs / 1_500L).toInt()

    val progression = listOf(
        listOf(60, 64, 67),
        listOf(57, 60, 64),
        listOf(55, 59, 62, 67),
        listOf(53, 57, 60)
    )

    val notes = progression.flatMapIndexed { index, chord ->
        chord.map { midi ->
            val state = when ((seed + midi + index) % 4) {
                0 -> NoteState.NONE
                1 -> NoteState.CORRECT
                2 -> NoteState.WRONG
                else -> NoteState.LATE
            }
            NoteEvent(
                midi = midi,
                startBeat = index * 2f,
                duration = listOf(4f, 2f, 1f, 0.5f, 0.25f)[(index + midi) % 5],
                expected = true,
                state = state,
                staff = staffForExercise(midi, HandMode.BOTH),
                accidental = NoteAccidental.NONE
            )
        }
    }

    val chords = progression.mapIndexed { idx, chordNotes ->
        val chordStaff = if (chordNotes.all { it < 60 }) StaffType.BASS else StaffType.TREBLE
        Chord(
            name = formatChordLabel(chordNotes, 0),
            notes = chordNotes,
            startBeat = idx * 2f,
            staff = chordStaff
        )
    }

    return GameState(
        levelTitle = "C - Mixed Practice",
        elapsedTime = (phase * 1_000L) + (nowMs % 1_000L),
        bpm = if (phase > 1) 60f + phase * 2f else 0f,
        notes = notes,
        chords = chords,
        pedalMarks = listOf(
            PedalMark(startBeat = 0f, action = PedalAction.PRESS, state = NoteState.NONE),
            PedalMark(startBeat = 4f, action = PedalAction.RELEASE, state = NoteState.NONE)
        ),
        currentBeat = phase / 2f,
        musicalKey = 0
    )
}
