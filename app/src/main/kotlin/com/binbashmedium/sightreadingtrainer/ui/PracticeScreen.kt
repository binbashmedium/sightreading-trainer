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
                val bpmText = if (gameState.bpm > 0f) "${gameState.bpm.toInt()} BPM" else "– BPM"
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

        val clefAreaWidth = lineSpacing * 3f
        val leftPadding = lineSpacing * 0.5f

        val (sharps, flats) = KEY_SIGNATURES.getOrElse(gameState.musicalKey) { 0 to 0 }
        val numAccidentals = sharps + flats
        val keySigWidth = if (numAccidentals > 0) numAccidentals * lineSpacing * 0.8f + lineSpacing * 0.5f else 0f

        val startX = leftPadding + clefAreaWidth + keySigWidth
        val beatWidth = max(30f, (size.width - startX - lineSpacing) / (gameState.chords.size.coerceAtLeast(1) * 2f + 1f))

        val black = Color(0xFF111111)
        val staffStroke = 2f

        // ── Staff lines ──────────────────────────────────────────────────────────
        val staffEndX = size.width - lineSpacing
        for (i in 0..4) {
            drawLine(black, Offset(startX, trebleTopY + i * lineSpacing), Offset(staffEndX, trebleTopY + i * lineSpacing), strokeWidth = staffStroke)
        }
        for (i in 0..4) {
            drawLine(black, Offset(startX, bassTopY + i * lineSpacing), Offset(staffEndX, bassTopY + i * lineSpacing), strokeWidth = staffStroke)
        }

        // Vertical brace connecting both staves
        drawLine(
            color = black,
            start = Offset(startX, trebleTopY),
            end = Offset(startX, bassTopY + 4 * lineSpacing),
            strokeWidth = 3f
        )

        // ── Clef symbols ─────────────────────────────────────────────────────────
        val clefPaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = lineSpacing * 4.8f
            isAntiAlias = true
        }
        val clefX = leftPadding + lineSpacing * 0.2f
        // Treble clef: glyph baseline should sit so the curl wraps around the G4 line (trebleTopY + 3*lineSpacing)
        drawContext.canvas.nativeCanvas.drawText("𝄞", clefX, trebleTopY + lineSpacing * 3.5f, clefPaint)

        val bassClefPaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = lineSpacing * 3.5f
            isAntiAlias = true
        }
        // Bass clef: baseline at the F3 line (bassTopY + lineSpacing)
        drawContext.canvas.nativeCanvas.drawText("𝄢", clefX, bassTopY + lineSpacing * 2.8f, bassClefPaint)

        // ── Key signature ─────────────────────────────────────────────────────────
        if (numAccidentals > 0) {
            val accPaint = Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.BLACK
                textSize = lineSpacing * 1.6f
                isAntiAlias = true
            }
            val keySigX0 = leftPadding + clefAreaWidth
            val accSpacing = lineSpacing * 0.8f
            val symbol = if (sharps > 0) "♯" else "♭"
            val trebleSteps = if (sharps > 0) TREBLE_SHARP_STEPS else TREBLE_FLAT_STEPS
            val bassSteps   = if (sharps > 0) BASS_SHARP_STEPS   else BASS_FLAT_STEPS

            repeat(numAccidentals) { i ->
                val x = keySigX0 + i * accSpacing

                val trebleStep = trebleSteps[i]
                val trebleY = (trebleTopY + 4f * lineSpacing) - (trebleStep - 30) * lineSpacing / 2f
                drawContext.canvas.nativeCanvas.drawText(symbol, x, trebleY + lineSpacing * 0.4f, accPaint)

                val bassStep = bassSteps[i]
                val bassY = (bassTopY + 4f * lineSpacing) - (bassStep - 18) * lineSpacing / 2f
                drawContext.canvas.nativeCanvas.drawText(symbol, x, bassY + lineSpacing * 0.4f, accPaint)
            }
        }

        // ── Beat separator lines ──────────────────────────────────────────────────
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
                chord.name, x + 4f, trebleTopY - lineSpacing * 1.2f, labelPaint
            )
        }

        // ── Notes (with ledger lines) ─────────────────────────────────────────────
        // Treble staff: bottom line step=30, top line step=38
        // Bass staff:   bottom line step=18, top line step=26
        val trebleBottomStep = 30
        val trebleTopStep = 38
        val bassBottomStep = 18
        val bassTopStep = 26

        gameState.notes.forEach { note ->
            val noteX = beatToX(note.startBeat, startX, beatWidth)
            val noteY = midiToGrandStaffY(note.midi, note.staff, trebleTopY, bassTopY, lineSpacing)
            val noteColor = when (note.state) {
                NoteState.NONE    -> Color.Black
                NoteState.CORRECT -> Color(0xFF2E7D32)
                NoteState.WRONG   -> Color(0xFFC62828)
                NoteState.LATE    -> Color(0xFFF9A825)
            }

            val step = midiToDiatonicStep(note.midi)
            val (bottomStep, topStep) = when (note.staff) {
                StaffType.TREBLE -> trebleBottomStep to trebleTopStep
                StaffType.BASS   -> bassBottomStep to bassTopStep
            }

            // Ledger lines below
            ledgerStepsBelow(step, bottomStep).forEach { l ->
                val lY = midiToGrandStaffY(
                    midiFromDiatonicStep(l), note.staff, trebleTopY, bassTopY, lineSpacing
                )
                drawLine(
                    color = black,
                    start = Offset(noteX - lineSpacing * 0.75f, lY),
                    end = Offset(noteX + lineSpacing * 0.75f, lY),
                    strokeWidth = staffStroke
                )
            }

            // Ledger lines above
            ledgerStepsAbove(step, topStep).forEach { l ->
                val lY = midiToGrandStaffY(
                    midiFromDiatonicStep(l), note.staff, trebleTopY, bassTopY, lineSpacing
                )
                drawLine(
                    color = black,
                    start = Offset(noteX - lineSpacing * 0.75f, lY),
                    end = Offset(noteX + lineSpacing * 0.75f, lY),
                    strokeWidth = staffStroke
                )
            }

            // Note head
            val glyph = durationToGlyphType(note.duration)
            val isHollow = glyph == NoteGlyphType.WHOLE || glyph == NoteGlyphType.HALF
            drawOval(
                color = noteColor,
                topLeft = Offset(noteX - lineSpacing * 0.55f, noteY - lineSpacing * 0.38f),
                size = Size(lineSpacing * 1.1f, lineSpacing * 0.76f),
                style = if (isHollow) Stroke(width = 2.4f) else Fill
            )

            // Stem and flags
            if (glyph != NoteGlyphType.WHOLE) {
                val stemX = noteX + lineSpacing * 0.5f
                drawLine(
                    color = noteColor,
                    start = Offset(stemX, noteY),
                    end = Offset(stemX, noteY - lineSpacing * 2.8f),
                    strokeWidth = 2f
                )
                val flagCount = when (glyph) {
                    NoteGlyphType.EIGHTH    -> 1
                    NoteGlyphType.SIXTEENTH -> 2
                    else                   -> 0
                }
                repeat(flagCount) { flagIndex ->
                    val fy = noteY - lineSpacing * (2.7f - flagIndex * 0.7f)
                    drawLine(
                        color = noteColor,
                        start = Offset(stemX, fy),
                        end = Offset(stemX + lineSpacing * 0.85f, fy + lineSpacing * 0.5f),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // ── Cursor (static at current expected beat) ──────────────────────────────
        val cursorX = beatToX(gameState.currentBeat, startX, beatWidth)
        drawLine(
            color = Color.Red,
            start = Offset(cursorX, trebleTopY - lineSpacing),
            end = Offset(cursorX, bassTopY + lineSpacing * 4.5f),
            strokeWidth = 3f
        )
    }
}

/**
 * Approximate MIDI note from a diatonic step (used only for ledger-line Y calculation).
 * Returns the MIDI for the natural note at that diatonic step (no accidentals).
 */
private fun midiFromDiatonicStep(step: Int): Int {
    val octave = step / 7
    val noteIdx = step % 7
    val semitoneOffsets = intArrayOf(0, 2, 4, 5, 7, 9, 11) // C D E F G A B
    return (octave + 1) * 12 + semitoneOffsets[noteIdx]
}

private fun com.binbashmedium.sightreadingtrainer.domain.model.PracticeState.toGameState(nowMs: Long): GameState {
    val elapsed = (nowMs - startTimeMs).coerceAtLeast(0L)
    val keyName = KEY_NAMES.getOrElse(exercise.musicalKey) { "C" }
    val levelDesc = when (exercise.expectedNotes.firstOrNull()?.size) {
        1    -> "Single Notes"
        2    -> "Intervals"
        3    -> "Triads / Progression"
        else -> "Practice"
    }
    val levelTitle = "$keyName · $levelDesc"

    val expectedNotes = exercise.expectedNotes.flatMapIndexed { beatIndex, chordNotes ->
        chordNotes.map { midi ->
            NoteEvent(
                midi = midi,
                startBeat = beatIndex.toFloat() * 2f,
                duration = 1f,
                expected = true,
                state = when (resultByBeat[beatIndex]) {
                    is MatchResult.Correct   -> NoteState.CORRECT
                    is MatchResult.Incorrect -> NoteState.WRONG
                    is MatchResult.TooLate   -> NoteState.LATE
                    else                     -> NoteState.NONE
                },
                staff = if (midi >= 60) StaffType.TREBLE else StaffType.BASS
            )
        }
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
        bpm = bpm,
        notes = expectedNotes,
        chords = chords,
        currentBeat = exercise.currentIndex.toFloat() * 2f,
        musicalKey = exercise.musicalKey
    )
}

private fun chordNameFromMidi(notes: List<Int>): String {
    if (notes.isEmpty()) return "?"
    val names = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")
    val root = names[((notes.minOrNull() ?: 60) % 12 + 12) % 12]
    return when (notes.size) {
        1    -> root
        2    -> "$root int"
        3    -> "$root triad"
        else -> "$root ${notes.size}n"
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
                0    -> NoteState.NONE
                1    -> NoteState.CORRECT
                2    -> NoteState.WRONG
                else -> NoteState.LATE
            }
            NoteEvent(
                midi = midi,
                startBeat = index * 2f,
                duration = listOf(4f, 2f, 1f, 0.5f, 0.25f)[(index + midi) % 5],
                expected = true,
                state = state,
                staff = if (midi >= 60) StaffType.TREBLE else StaffType.BASS
            )
        }
    }

    val chords = progression.mapIndexed { idx, chordNotes ->
        Chord(
            name = chordNameFromMidi(chordNotes),
            notes = chordNotes,
            startBeat = idx * 2f
        )
    }

    return GameState(
        levelTitle = "C · Triads / Progression",
        elapsedTime = (phase * 1_000L) + (nowMs % 1_000L),
        score = 100 + (phase * 15),
        bpm = if (phase > 1) 60f + phase * 2f else 0f,
        notes = notes,
        chords = chords,
        currentBeat = phase / 2f,
        musicalKey = 0
    )
}
