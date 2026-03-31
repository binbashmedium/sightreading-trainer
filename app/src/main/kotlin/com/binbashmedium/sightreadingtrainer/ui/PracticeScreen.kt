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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteAccidental
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun PracticeScreen(
    navController: NavController,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.startSession()
    }

    val state by viewModel.practiceState.collectAsState()
    val sessionResult by viewModel.sessionResult.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val currentNow = System.currentTimeMillis()
            now = currentNow
            viewModel.finalizeIfTimedOut(currentNow)
            delay(500)
        }
    }

    val gameState = remember(state, now) {
        state?.toGameState(now) ?: generateExampleGameState(now)
    }

    val isComplete = sessionResult != null

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FB))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderCard(gameState = gameState)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                GrandStaffCanvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    gameState = gameState
                )
            }

            Button(
                onClick = { viewModel.reloadSession() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("New Exercise")
            }
        }

        if (isComplete) {
            val result = sessionResult!!
            SessionCompleteOverlay(
                score = result.score,
                highScore = result.highScore,
                isNewHighScore = result.isNewHighScore,
                correctNotes = result.correctNotes,
                wrongNotes = result.wrongNotes,
                bpm = gameState.bpm,
                onRestart = { viewModel.reloadSession() }
            )
        }
    }
}

@Composable
private fun SessionCompleteOverlay(
    score: Int,
    highScore: Int,
    isNewHighScore: Boolean,
    correctNotes: Int,
    wrongNotes: Int,
    bpm: Float,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2748)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Exercise Complete!",
                    color = Color(0xFFF2F6FF),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$score pts",
                    color = Color(0xFFFFD700),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Highscore: $highScore pts",
                    color = if (isNewHighScore) Color(0xFFB7FFB7) else Color(0xFFB0C4DE),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Correct / Wrong: $correctNotes / $wrongNotes",
                    color = Color(0xFFB0C4DE),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                if (bpm > 0f) {
                    Text(
                        text = "${bpm.toInt()} BPM",
                        color = Color(0xFFB0C4DE),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("New Exercise", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(gameState: GameState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2748)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = gameState.levelTitle,
                color = Color(0xFFF2F6FF),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(2f)
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = formatElapsedTime(gameState.elapsedTime),
                    color = Color(0xFFF2F6FF),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val bpmText = if (gameState.bpm > 0f) "${gameState.bpm.toInt()} BPM" else "- BPM"
                Text(
                    text = bpmText,
                    color = Color(0xFFF2F6FF),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = "${gameState.score} pts",
                    color = Color(0xFFF2F6FF),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GrandStaffCanvas(
    modifier: Modifier = Modifier,
    gameState: GameState
) {
    Canvas(modifier = modifier) {
        val lineSpacing = size.height / 18f
        val trebleTopY = lineSpacing * 2.5f
        val bassTopY = lineSpacing * 10f

        val (sharps, flats) = KEY_SIGNATURES.getOrElse(gameState.musicalKey) { 0 to 0 }
        val numAccidentals = sharps + flats
        val layout = grandStaffLayoutMetrics(lineSpacing, numAccidentals)
        val staffStartX = lineSpacing * 0.5f
        val clefAreaWidth = layout.clefAreaWidth
        val postClefGap = layout.postClefGap
        val accidentalSpacing = layout.accidentalSpacing
        val keySigWidth = layout.keySignatureWidth

        val startX = staffStartX + clefAreaWidth + postClefGap + keySigWidth
        val beatWidth = max(
            20f,
            (size.width - startX - lineSpacing) / (gameState.chords.size.coerceAtLeast(1) * 2f + 1f)
        )

        val black = Color(0xFF111111)
        val staffStroke = 2f
        val staffEndX = size.width - lineSpacing
        val noteAccidentalPaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = lineSpacing * NOTE_ACCIDENTAL_TEXT_SIZE_RATIO
            isAntiAlias = true
        }

        for (i in 0..4) {
            drawLine(
                color = black,
                start = Offset(staffStartX, trebleTopY + i * lineSpacing),
                end = Offset(staffEndX, trebleTopY + i * lineSpacing),
                strokeWidth = staffStroke
            )
        }
        for (i in 0..4) {
            drawLine(
                color = black,
                start = Offset(staffStartX, bassTopY + i * lineSpacing),
                end = Offset(staffEndX, bassTopY + i * lineSpacing),
                strokeWidth = staffStroke
            )
        }

        drawLine(
            color = black,
            start = Offset(staffStartX, trebleTopY),
            end = Offset(staffStartX, bassTopY + 4f * lineSpacing),
            strokeWidth = 3f
        )

        val trebleClefPaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = layout.trebleClefTextSize
            isAntiAlias = true
        }
        val clefX = staffStartX + lineSpacing * 0.35f
        val bassClefGeometry = bassClefGeometry(clefX, trebleTopY, bassTopY, lineSpacing)
        val bassClefPaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = bassClefGeometry.textSize
            isAntiAlias = true
        }
        val trebleAnchorY = staffLineYForStep(
            TREBLE_G_LINE_STEP,
            StaffType.TREBLE,
            trebleTopY,
            bassTopY,
            lineSpacing
        )
        drawContext.canvas.nativeCanvas.drawText("\uD834\uDD1E", clefX, trebleClefBaselineY(trebleAnchorY, lineSpacing), trebleClefPaint)
        // The 𝄢 Unicode glyph already includes both dots; no extra circles needed.
        drawContext.canvas.nativeCanvas.drawText("\uD834\uDD22", bassClefGeometry.glyphX, bassClefGeometry.baselineY, bassClefPaint)

        if (numAccidentals > 0) {
            val accidentalPaint = Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.BLACK
                textSize = layout.accidentalTextSize
                isAntiAlias = true
            }
            val keySigX0 = staffStartX + clefAreaWidth - layout.keySignatureStartOffset
            val symbol = if (sharps > 0) "\u266F" else "\u266D"
            val trebleSteps = if (sharps > 0) TREBLE_SHARP_STEPS else TREBLE_FLAT_STEPS
            val bassSteps = if (sharps > 0) BASS_SHARP_STEPS else BASS_FLAT_STEPS

            repeat(numAccidentals) { index ->
                val x = keySigX0 + index * accidentalSpacing
                val trebleY = staffLineYForStep(trebleSteps[index], StaffType.TREBLE, trebleTopY, bassTopY, lineSpacing)
                val bassY = staffLineYForStep(bassSteps[index], StaffType.BASS, trebleTopY, bassTopY, lineSpacing)
                drawContext.canvas.nativeCanvas.drawText(symbol, x, trebleY + lineSpacing * 0.4f, accidentalPaint)
                drawContext.canvas.nativeCanvas.drawText(symbol, x, bassY + lineSpacing * 0.4f, accidentalPaint)
            }
        }

        // Label text size scales with beat width so labels never overlap.
        val labelTextSize = (beatWidth * 0.5f).coerceIn(10f, lineSpacing * 0.42f)
        val labelPaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = labelTextSize
            isAntiAlias = true
        }
        val trebleLabelY = trebleTopY - lineSpacing * 0.55f   // above treble staff
        val bassLabelY   = bassTopY   + lineSpacing * 5.3f    // below bass staff

        gameState.chords.forEach { chord ->
            val x = beatToX(chord.startBeat, startX, beatWidth)
            drawLine(
                color = Color(0x447E8798),
                start = Offset(x, trebleTopY - lineSpacing * 0.6f),
                end = Offset(x, bassTopY + lineSpacing * 4f),
                strokeWidth = 1f
            )
            // Use the compact label (no Roman numeral) to keep text within beat width.
            val label = formatChordLabelShort(chord.notes)
            val labelY = if (chord.staff == StaffType.BASS) bassLabelY else trebleLabelY
            drawContext.canvas.nativeCanvas.drawText(label, x + 2f, labelY, labelPaint)
        }

        val noteHeadWidth = lineSpacing * 1.1f
        val noteHeadHeight = lineSpacing * 0.76f
        gameState.notes
            .groupBy { it.startBeat to it.staff }
            .toSortedMap(compareBy<Pair<Float, StaffType>> { it.first }.thenBy { it.second.ordinal })
            .values
            .forEach { chordNotes ->
                val baseX = beatToX(chordNotes.first().startBeat, startX, beatWidth)
                val staff = chordNotes.first().staff
                val glyph = durationToGlyphType(chordNotes.first().duration)
                val sortedNotes = chordNotes.sortedBy { displayDiatonicStep(it.midi, it.accidental) }
                val steps = sortedNotes.map { displayDiatonicStep(it.midi, it.accidental) }
                val direction = stemDirectionForSteps(steps, staff)
                val xOffsets = chordNoteheadOffsets(steps, direction, lineSpacing)
                val accidentalColumns = accidentalColumnsForSteps(steps)
                val noteCenters = sortedNotes.mapIndexed { index, note ->
                    val noteX = baseX + xOffsets[index]
                    val noteY = midiToGrandStaffY(note.midi, note.staff, trebleTopY, bassTopY, lineSpacing, note.accidental)
                    Triple(note, noteX, noteY)
                }

                noteCenters.forEachIndexed { index, (note, noteX, noteY) ->
                    val step = displayDiatonicStep(note.midi, note.accidental)
                    val (bottomStep, topStep) = when (note.staff) {
                        StaffType.TREBLE -> TREBLE_BOTTOM_LINE_STEP to TREBLE_TOP_LINE_STEP
                        StaffType.BASS -> BASS_BOTTOM_LINE_STEP to BASS_TOP_LINE_STEP
                    }

                    ledgerStepsBelow(step, bottomStep).forEach { ledgerStep ->
                        val ledgerY = staffLineYForStep(ledgerStep, note.staff, trebleTopY, bassTopY, lineSpacing)
                        drawLine(
                            color = black,
                            start = Offset(noteX - lineSpacing * 0.75f, ledgerY),
                            end = Offset(noteX + lineSpacing * 0.75f, ledgerY),
                            strokeWidth = staffStroke
                        )
                    }

                    ledgerStepsAbove(step, topStep).forEach { ledgerStep ->
                        val ledgerY = staffLineYForStep(ledgerStep, note.staff, trebleTopY, bassTopY, lineSpacing)
                        drawLine(
                            color = black,
                            start = Offset(noteX - lineSpacing * 0.75f, ledgerY),
                            end = Offset(noteX + lineSpacing * 0.75f, ledgerY),
                            strokeWidth = staffStroke
                        )
                    }

                    val isHollow = glyph == NoteGlyphType.WHOLE || glyph == NoteGlyphType.HALF
                    val noteColor = colorForNoteState(note.state)
                    drawOval(
                        color = noteColor,
                        topLeft = Offset(noteX - noteHeadWidth / 2f, noteY - noteHeadHeight / 2f),
                        size = Size(noteHeadWidth, noteHeadHeight),
                        style = if (isHollow) Stroke(width = 2.4f) else Fill
                    )

                    noteAccidentalSymbol(note.accidental)?.let { symbol ->
                        val accidentalX = noteX - lineSpacing * (1.15f + accidentalColumns[index] * 0.58f)
                        drawContext.canvas.nativeCanvas.drawText(
                            symbol,
                            accidentalX,
                            noteY + lineSpacing * 0.34f,
                            noteAccidentalPaint
                        )
                    }
                }

                if (glyph != NoteGlyphType.WHOLE) {
                    val middleY = staffLineYForStep(
                        middleLineStepForStaff(staff),
                        staff,
                        trebleTopY,
                        bassTopY,
                        lineSpacing
                    )
                    val stem = stemGeometryForChord(
                        noteXs = noteCenters.map { it.second },
                        noteYs = noteCenters.map { it.third },
                        direction = direction,
                        middleLineY = middleY,
                        lineSpacing = lineSpacing,
                        noteHeadWidth = noteHeadWidth
                    )
                    val stemColor = stemColorForStates(noteCenters.map { it.first.state })
                    drawLine(
                        color = stemColor,
                        start = Offset(stem.x, stem.startY),
                        end = Offset(stem.x, stem.endY),
                        strokeWidth = 2f
                    )
                    val flagCount = when (glyph) {
                        NoteGlyphType.EIGHTH -> 1
                        NoteGlyphType.SIXTEENTH -> 2
                        else -> 0
                    }
                    repeat(flagCount) { flagIndex ->
                        val flagY = if (direction == StemDirection.UP) {
                            stem.endY + lineSpacing * (0.1f + flagIndex * 0.7f)
                        } else {
                            stem.endY - lineSpacing * (0.1f + flagIndex * 0.7f)
                        }
                        val flagEnd = if (direction == StemDirection.UP) {
                            Offset(stem.x + lineSpacing * 0.85f, flagY + lineSpacing * 0.5f)
                        } else {
                            Offset(stem.x - lineSpacing * 0.85f, flagY - lineSpacing * 0.5f)
                        }
                        drawLine(
                            color = stemColor,
                            start = Offset(stem.x, flagY),
                            end = flagEnd,
                            strokeWidth = 2f
                        )
                    }
                }
            }

        val pedalPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = lineSpacing * 0.85f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val pedalY = bassTopY + lineSpacing * 7.2f
        gameState.pedalMarks.forEach { pedalMark ->
            val color = when (pedalMark.state) {
                NoteState.NONE -> android.graphics.Color.BLACK
                NoteState.CORRECT -> android.graphics.Color.parseColor("#2E7D32")
                NoteState.WRONG -> android.graphics.Color.parseColor("#C62828")
                NoteState.LATE -> android.graphics.Color.parseColor("#F9A825")
            }
            pedalPaint.color = color
            pedalMarkText(pedalMark.action)?.let { label ->
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    beatToX(pedalMark.startBeat, startX, beatWidth) + beatWidth * 0.3f,
                    pedalY,
                    pedalPaint
                )
            }
        }

        val cursorX = beatToX(gameState.currentBeat, startX, beatWidth)
        drawLine(
            color = Color.Red,
            start = Offset(cursorX, trebleTopY - lineSpacing),
            end = Offset(cursorX, bassTopY + lineSpacing * 4.5f),
            strokeWidth = 3f
        )
    }
}

private fun com.binbashmedium.sightreadingtrainer.domain.model.PracticeState.toGameState(nowMs: Long): GameState {
    val elapsed = (nowMs - startTimeMs).coerceAtLeast(0L)
    val keyName = KEY_NAMES.getOrElse(exercise.musicalKey) { "C" }
    val sizes = exercise.steps.map { it.notes.size }.filter { it > 0 }.distinct().sorted()
    val levelDesc = when {
        sizes.size > 1 -> "Mixed Practice"
        sizes.singleOrNull() == 1 -> "Single Notes"
        sizes.singleOrNull() == 2 -> "Intervals"
        sizes.singleOrNull() == 3 -> "Triads"
        sizes.singleOrNull() == 4 -> "Sevenths"
        sizes.singleOrNull() == 5 -> "Ninths"
        else -> "Practice"
    }
    val levelTitle = "$keyName - $levelDesc"

    val expectedNotes = exercise.steps.flatMapIndexed { beatIndex, step ->
        val stepBeat = beatIndex.toFloat() * 2f
        val snapshot = inputByBeat[beatIndex]
        val noteOutcome = if (snapshot != null) {
            classifyStepNotes(step.notes, snapshot.playedNotes)
        } else {
            StepNoteDisplayOutcome(
                expectedStates = List(step.notes.size) { NoteState.NONE },
                extraNotes = emptyList()
            )
        }
        val expectedEvents = step.notes.mapIndexed { noteIndex, midi ->
            NoteEvent(
                midi = midi,
                startBeat = stepBeat,
                duration = 1f,
                expected = true,
                state = noteOutcome.expectedStates.getOrElse(noteIndex) { NoteState.NONE },
                staff = staffForExercise(midi, exercise.handMode),
                accidental = step.noteAccidentals.getOrElse(noteIndex) { NoteAccidental.NONE }
            )
        }
        val extraEvents = noteOutcome.extraNotes.map { midi ->
            NoteEvent(
                midi = midi,
                startBeat = stepBeat,
                duration = 1f,
                expected = false,
                state = NoteState.LATE,
                staff = staffForExercise(midi, exercise.handMode),
                accidental = accidentalForPlayedMidi(midi)
            )
        }
        expectedEvents + extraEvents
    }

    val chords = exercise.steps.mapIndexedNotNull { idx, step ->
        val notes = step.notes
        if (notes.isEmpty()) return@mapIndexedNotNull null
        val chordStaff = if (notes.all { staffForExercise(it, exercise.handMode) == StaffType.BASS })
            StaffType.BASS else StaffType.TREBLE
        Chord(
            name = formatChordLabel(notes, exercise.musicalKey),
            notes = notes,
            startBeat = idx.toFloat() * 2f,
            staff = chordStaff
        )
    }

    val pedalMarks = exercise.steps.flatMapIndexed { idx, step ->
        val beat = idx.toFloat() * 2f
        val snapshot = inputByBeat[idx]
        val marks = mutableListOf<PedalMark>()
        if (step.pedalAction != PedalAction.NONE) {
            val state = when {
                snapshot == null -> NoteState.NONE
                isExpectedPedalSatisfied(step.pedalAction, snapshot) -> NoteState.CORRECT
                else -> NoteState.WRONG
            }
            marks += PedalMark(startBeat = beat, action = step.pedalAction, state = state)
        }
        if (snapshot != null && snapshot.playedPedalAction != PedalAction.NONE && snapshot.playedPedalAction != step.pedalAction) {
            marks += PedalMark(startBeat = beat, action = snapshot.playedPedalAction, state = NoteState.LATE)
        }
        marks
    }

    return GameState(
        levelTitle = levelTitle,
        elapsedTime = elapsed,
        score = score,
        bpm = bpm,
        notes = expectedNotes,
        chords = chords,
        pedalMarks = pedalMarks,
        currentBeat = exercise.currentIndex.toFloat() * 2f,
        musicalKey = exercise.musicalKey
    )
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
        score = 100 + (phase * 15),
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

internal fun colorForNoteState(state: NoteState): Color = when (state) {
    NoteState.NONE -> Color.Black
    NoteState.CORRECT -> Color(0xFF2E7D32)
    NoteState.WRONG -> Color(0xFFC62828)
    NoteState.LATE -> Color(0xFFF9A825)
}

internal fun stemColorForStates(states: List<NoteState>): Color {
    if (states.isEmpty()) return Color.Black
    val first = states.first()
    return if (states.all { it == first }) colorForNoteState(first) else Color.Black
}
