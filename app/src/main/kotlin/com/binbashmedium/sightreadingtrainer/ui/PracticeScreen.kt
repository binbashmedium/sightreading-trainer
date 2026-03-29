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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun PracticeScreen(
    navController: NavController,
    viewModel: PracticeViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.startSession(mainViewModel.createExercise())
    }

    val state by viewModel.practiceState.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var paused by remember { mutableStateOf(false) }

    LaunchedEffect(paused) {
        while (true) {
            if (!paused) {
                now = System.currentTimeMillis()
            }
            delay(250)
        }
    }

    val gameState = remember(state, now) {
        state?.toGameState(now) ?: generateExampleGameState(now)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { paused = !paused },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (paused) "Resume" else "Pause")
            }
            Button(
                onClick = { /* Hook for hint action */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Hint")
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = gameState.levelTitle,
                color = Color(0xFFF2F6FF),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = formatElapsedTime(gameState.elapsedTime),
                    color = Color(0xFFF2F6FF),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = gameState.score.toString(),
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
        val trebleTopY = lineSpacing * 3f
        val bassTopY = lineSpacing * 10f

        val leftPadding = lineSpacing * 3f
        val startX = leftPadding + lineSpacing * 3f
        val beatWidth = max(30f, size.width / 24f)

        val black = Color(0xFF111111)

        // Grand staff lines: exactly one treble + one bass staff.
        for (i in 0..4) {
            val y = trebleTopY + i * lineSpacing
            drawLine(black, Offset(startX, y), Offset(size.width - lineSpacing, y), strokeWidth = 2f)
        }
        for (i in 0..4) {
            val y = bassTopY + i * lineSpacing
            drawLine(black, Offset(startX, y), Offset(size.width - lineSpacing, y), strokeWidth = 2f)
        }

        // Connect left side for one grand staff system.
        drawLine(
            color = black,
            start = Offset(startX, trebleTopY),
            end = Offset(startX, bassTopY + 4 * lineSpacing),
            strokeWidth = 3f
        )

        // Clef symbols.
        val textPaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = lineSpacing * 5f
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText("𝄞", leftPadding, trebleTopY + lineSpacing * 3.7f, textPaint)
        drawContext.canvas.nativeCanvas.drawText("𝄢", leftPadding, bassTopY + lineSpacing * 3.6f, textPaint)

        // Bar lines + chord labels (dynamic).
        gameState.chords.forEach { chord ->
            val x = beatToX(chord.startBeat, startX, beatWidth)
            drawLine(
                color = Color(0xFF7E8798),
                start = Offset(x, trebleTopY - lineSpacing * 1.1f),
                end = Offset(x, bassTopY + lineSpacing * 4f),
                strokeWidth = 1.2f
            )
            drawContext.canvas.nativeCanvas.drawText(
                chord.name,
                x + 6f,
                trebleTopY - lineSpacing * 1.6f,
                Paint().asFrameworkPaint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = lineSpacing * 1.25f
                    isAntiAlias = true
                }
            )
        }

        // Notes (including user-played unexpected notes).
        gameState.notes.forEach { note ->
            val noteX = beatToX(note.startBeat, startX, beatWidth)
            val noteY = midiToGrandStaffY(note.midi, trebleTopY, bassTopY, lineSpacing)
            val noteColor = when (note.state) {
                NoteState.NONE -> Color.Black
                NoteState.CORRECT -> Color(0xFF2E7D32)
                NoteState.WRONG -> Color(0xFFC62828)
                NoteState.LATE -> Color(0xFFF9A825)
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
                    val y = noteY - lineSpacing * (2.7f - (flagIndex * 0.7f))
                    drawLine(
                        color = noteColor,
                        start = Offset(stemX, y),
                        end = Offset(stemX + lineSpacing * 0.85f, y + lineSpacing * 0.5f),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // Time cursor (dynamic red vertical line).
        val cursorX = beatToX(gameState.currentBeat, startX, beatWidth)
        drawLine(
            color = Color.Red,
            start = Offset(cursorX, trebleTopY - lineSpacing),
            end = Offset(cursorX, bassTopY + lineSpacing * 4.2f),
            strokeWidth = 3f
        )
    }
}

private fun com.binbashmedium.sightreadingtrainer.domain.model.PracticeState.toGameState(nowMs: Long): GameState {
    val elapsed = (nowMs - startTimeMs).coerceAtLeast(0L)
    val levelTitle = "Level ${exercise.currentIndex + 1}: Chord Practice"

    val expectedNotes = exercise.expectedNotes.flatMapIndexed { beatIndex, chordNotes ->
        chordNotes.map { midi ->
            NoteEvent(
                midi = midi,
                startBeat = beatIndex.toFloat() * 2f,
                duration = 1f,
                expected = true,
                state = when {
                    beatIndex < exercise.currentIndex -> when (lastResult) {
                        is MatchResult.Correct -> NoteState.CORRECT
                        is MatchResult.Incorrect -> NoteState.WRONG
                        is MatchResult.TooLate -> NoteState.LATE
                        else -> NoteState.NONE
                    }
                    else -> NoteState.NONE
                }
            )
        }
    }

    val accidentalWrong = if (lastResult is MatchResult.Incorrect && exercise.currentChord?.isNotEmpty() == true) {
        listOf(
            NoteEvent(
                midi = exercise.currentChord.first() + 1,
                startBeat = exercise.currentIndex.toFloat() * 2f,
                duration = 1f,
                expected = false,
                state = NoteState.WRONG
            )
        )
    } else {
        emptyList()
    }

    val chords = exercise.expectedNotes.mapIndexed { idx, notes ->
        Chord(
            name = chordNameFromMidi(notes),
            notes = notes,
            startBeat = idx.toFloat() * 2f
        )
    }

    return GameState(
        levelTitle = levelTitle,
        elapsedTime = elapsed,
        score = score,
        notes = expectedNotes + accidentalWrong,
        chords = chords,
        currentBeat = (elapsed / 1000f).coerceAtMost(max(1f, exercise.expectedNotes.size * 2f))
    )
}

private fun chordNameFromMidi(notes: List<Int>): String {
    if (notes.isEmpty()) return "Unknown"
    val names = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val root = names[((notes.minOrNull() ?: 60) % 12 + 12) % 12]
    return when (notes.size) {
        1 -> "$root note"
        2 -> "$root dyad"
        3 -> "$root triad"
        else -> "$root ${notes.size}-note chord"
    }
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
                state = state
            )
        }
    } + NoteEvent(
        midi = 66,
        startBeat = (phase / 2f),
        duration = 0.5f,
        expected = false,
        state = NoteState.WRONG
    )

    val chords = progression.mapIndexed { idx, chordNotes ->
        Chord(
            name = chordNameFromMidi(chordNotes),
            notes = chordNotes,
            startBeat = idx * 2f
        )
    }

    return GameState(
        levelTitle = "Level ${1 + (phase % 6)}: Sight Reading",
        elapsedTime = (phase * 1_000L) + (nowMs % 1_000L),
        score = 100 + (phase * 3),
        notes = notes,
        chords = chords,
        currentBeat = phase / 2f
    )
}
