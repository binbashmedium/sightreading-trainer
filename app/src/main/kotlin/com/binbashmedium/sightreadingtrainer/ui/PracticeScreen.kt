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
import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
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
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }

    val gameState = remember(state, now) {
        state?.toGameState(now) ?: generateExampleGameState(now)
    }

    val isComplete = state?.exercise?.isComplete == true

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
            SessionCompleteOverlay(
                score = gameState.score,
                bpm = gameState.bpm,
                onRestart = { viewModel.reloadSession() }
            )
        }
    }
}

@Composable
private fun SessionCompleteOverlay(score: Int, bpm: Float, onRestart: () -> Unit) {
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
            30f,
            (size.width - startX - lineSpacing) / (gameState.chords.size.coerceAtLeast(1) * 2f + 1f)
        )

        val black = Color(0xFF111111)
        val staffStroke = 2f
        val staffEndX = size.width - lineSpacing

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
        drawContext.canvas.nativeCanvas.drawText("\uD834\uDD22", bassClefGeometry.glyphX, bassClefGeometry.baselineY, bassClefPaint)
        drawCircle(
            color = black,
            radius = bassClefGeometry.dotRadius,
            center = Offset(bassClefGeometry.dotCenterX, bassClefGeometry.upperDotY)
        )
        drawCircle(
            color = black,
            radius = bassClefGeometry.dotRadius,
            center = Offset(bassClefGeometry.dotCenterX, bassClefGeometry.lowerDotY)
        )

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

        val labelPaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = lineSpacing * 1.1f
            isAntiAlias = true
        }
        gameState.chords.forEach { chord ->
            val x = beatToX(chord.startBeat, startX, beatWidth)
            drawLine(
                color = Color(0x447E8798),
                start = Offset(x, trebleTopY - lineSpacing * 0.8f),
                end = Offset(x, bassTopY + lineSpacing * 4f),
                strokeWidth = 1f
            )
            drawContext.canvas.nativeCanvas.drawText(
                chord.name,
                x + 4f,
                trebleTopY - lineSpacing * 1.2f,
                labelPaint
            )
        }

        gameState.notes.forEach { note ->
            val noteX = beatToX(note.startBeat, startX, beatWidth)
            val noteY = midiToGrandStaffY(note.midi, note.staff, trebleTopY, bassTopY, lineSpacing)
            val noteColor = when (note.state) {
                NoteState.NONE -> Color.Black
                NoteState.CORRECT -> Color(0xFF2E7D32)
                NoteState.WRONG -> Color(0xFFC62828)
                NoteState.LATE -> Color(0xFFF9A825)
            }

            val step = midiToDiatonicStep(note.midi)
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

            val glyph = durationToGlyphType(note.duration)
            val isHollow = glyph == NoteGlyphType.WHOLE || glyph == NoteGlyphType.HALF
            drawOval(
                color = noteColor,
                topLeft = Offset(noteX - lineSpacing * 0.55f, noteY - lineSpacing * 0.38f),
                size = Size(lineSpacing * 1.1f, lineSpacing * 0.76f),
                style = if (isHollow) Stroke(width = 2.4f) else Fill
            )

            if (glyph != NoteGlyphType.WHOLE) {
                val stemX = noteX + lineSpacing * 0.5f
                drawLine(
                    color = noteColor,
                    start = Offset(stemX, noteY),
                    end = Offset(stemX, noteY - lineSpacing * 2.8f),
                    strokeWidth = 2f
                )
                val flagCount = when (glyph) {
                    NoteGlyphType.EIGHTH -> 1
                    NoteGlyphType.SIXTEENTH -> 2
                    else -> 0
                }
                repeat(flagCount) { flagIndex ->
                    val flagY = noteY - lineSpacing * (2.7f - flagIndex * 0.7f)
                    drawLine(
                        color = noteColor,
                        start = Offset(stemX, flagY),
                        end = Offset(stemX + lineSpacing * 0.85f, flagY + lineSpacing * 0.5f),
                        strokeWidth = 2f
                    )
                }
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
    val sizes = exercise.expectedNotes.map { it.size }.distinct().sorted()
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

    val expectedNotes = exercise.expectedNotes.flatMapIndexed { beatIndex, chordNotes ->
        chordNotes.map { midi ->
            NoteEvent(
                midi = midi,
                startBeat = beatIndex.toFloat() * 2f,
                duration = 1f,
                expected = true,
                state = when (resultByBeat[beatIndex]) {
                    is MatchResult.Correct -> NoteState.CORRECT
                    is MatchResult.Incorrect -> NoteState.WRONG
                    is MatchResult.TooLate -> NoteState.LATE
                    else -> NoteState.NONE
                },
                staff = staffForExercise(midi, exercise.handMode)
            )
        }
    }

    val chords = exercise.expectedNotes.mapIndexed { idx, notes ->
        Chord(
            name = formatChordLabel(notes, exercise.musicalKey),
            notes = notes,
            startBeat = idx.toFloat() * 2f
        )
    }

    return GameState(
        levelTitle = levelTitle,
        elapsedTime = elapsed,
        score = score,
        bpm = bpm,
        notes = expectedNotes,
        chords = chords,
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
                staff = staffForExercise(midi, HandMode.BOTH)
            )
        }
    }

    val chords = progression.mapIndexed { idx, chordNotes ->
        Chord(
            name = formatChordLabel(chordNotes, 0),
            notes = chordNotes,
            startBeat = idx * 2f
        )
    }

    return GameState(
        levelTitle = "C - Mixed Practice",
        elapsedTime = (phase * 1_000L) + (nowMs % 1_000L),
        score = 100 + (phase * 15),
        bpm = if (phase > 1) 60f + phase * 2f else 0f,
        notes = notes,
        chords = chords,
        currentBeat = phase / 2f,
        musicalKey = 0
    )
}
