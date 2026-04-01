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

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteAccidental
import com.binbashmedium.sightreadingtrainer.domain.model.NoteValue
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
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

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
                if (isPortrait) {
                    // Portrait: 4 grand-staff rows, each covering BEATS_PER_ROW beat-units.
                    val page = beatToPage(gameState.currentBeat)
                    val pageStart = pageStartBeat(page)
                    Column(modifier = Modifier.fillMaxSize()) {
                        repeat(ROWS_PER_PAGE) { rowIdx ->
                            val rowStart = pageStart + rowIdx * BEATS_PER_ROW
                            val rowEnd = rowStart + BEATS_PER_ROW
                            GrandStaffCanvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                gameState = gameState,
                                startBeat = rowStart,
                                endBeat = rowEnd,
                                beatsPerMeasure = BEATS_PER_MEASURE_UNITS,
                                measureNumberLabel = rowMeasureLabel(rowStart)
                            )
                        }
                    }
                } else {
                    // Landscape: 4-measure window matching one portrait row.
                    val landscapePage = (gameState.currentBeat / BEATS_PER_ROW).toInt()
                    val landscapeStart = landscapePage * BEATS_PER_ROW
                    val landscapeEnd = landscapeStart + BEATS_PER_ROW
                    GrandStaffCanvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        gameState = gameState,
                        startBeat = landscapeStart,
                        endBeat = landscapeEnd,
                        beatsPerMeasure = BEATS_PER_MEASURE_UNITS,
                        measureNumberLabel = rowMeasureLabel(landscapeStart)
                    )
                }
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
    gameState: GameState,
    /** First beat-unit shown in this canvas (inclusive). 0 for landscape / row-start for portrait. */
    startBeat: Float = 0f,
    /** Last beat-unit shown in this canvas (exclusive). Float.MAX_VALUE means "show all". */
    endBeat: Float = Float.MAX_VALUE,
    /** Beat-units between bar lines (= BEATS_PER_MEASURE_UNITS for standard 4/4). */
    beatsPerMeasure: Float = BEATS_PER_MEASURE_UNITS,
    /** 1-based measure number shown top-left of each portrait row; null = hidden. */
    measureNumberLabel: Int? = null
) {
    val isPortraitRow = endBeat < Float.MAX_VALUE / 2f

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

        val noteStartX = staffStartX + clefAreaWidth + postClefGap + keySigWidth

        // ── Filter content to this row's beat range ──────────────────────────
        val rowNotes = if (isPortraitRow)
            gameState.notes.filter { it.startBeat >= startBeat && it.startBeat < endBeat }
        else gameState.notes

        val rowChords = if (isPortraitRow)
            gameState.chords.filter { it.startBeat >= startBeat && it.startBeat < endBeat }
        else gameState.chords

        val rowPedals = if (isPortraitRow)
            gameState.pedalMarks.filter { it.startBeat >= startBeat && it.startBeat < endBeat }
        else gameState.pedalMarks

        // ── Beat width: fixed range for portrait rows, chord-count for landscape ──
        val beatRange = if (isPortraitRow) {
            endBeat - startBeat
        } else {
            rowChords.size.coerceAtLeast(1) * 2f
        }
        val beatWidth = max(
            20f,
            (size.width - noteStartX - lineSpacing) / (beatRange + 1f)
        )

        // Local beat = note.startBeat - startBeat (so local 0 = left edge of this row)
        fun localBeat(beat: Float): Float = beat - startBeat
        fun beatX(localBeat: Float): Float = beatToX(localBeat, noteStartX, beatWidth)

        val black = Color(0xFF111111)
        val staffStroke = 2f
        val staffEndX = size.width - lineSpacing * 0.5f
        val noteAccidentalPaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = lineSpacing * NOTE_ACCIDENTAL_TEXT_SIZE_RATIO
            isAntiAlias = true
        }

        // ── Staff lines ───────────────────────────────────────────────────────
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

        // ── Left bar line (brace line spanning both staves) ───────────────────
        drawLine(
            color = black,
            start = Offset(staffStartX, trebleTopY),
            end = Offset(staffStartX, bassTopY + 4f * lineSpacing),
            strokeWidth = 3f
        )

        // ── Clefs ─────────────────────────────────────────────────────────────
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
            TREBLE_G_LINE_STEP, StaffType.TREBLE, trebleTopY, bassTopY, lineSpacing
        )
        drawContext.canvas.nativeCanvas.drawText(
            "\uD834\uDD1E", clefX, trebleClefBaselineY(trebleAnchorY, lineSpacing), trebleClefPaint
        )
        drawContext.canvas.nativeCanvas.drawText(
            "\uD834\uDD22", bassClefGeometry.glyphX, bassClefGeometry.baselineY, bassClefPaint
        )

        // ── Key signature ─────────────────────────────────────────────────────
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

        // ── Bar lines (measure boundaries) ───────────────────────────────────
        val staffTop = trebleTopY
        val staffBottom = bassTopY + 4f * lineSpacing
        var barLocal = beatsPerMeasure
        while (barLocal <= beatRange + 0.01f) {
            val barX = beatX(barLocal)
            val isEndBar = barLocal >= beatRange - 0.01f
            drawLine(
                color = black,
                start = Offset(barX, staffTop),
                end = Offset(barX, staffBottom),
                strokeWidth = if (isEndBar) 3f else 1.5f
            )
            barLocal += beatsPerMeasure
        }

        // ── Measure number label (portrait rows only) ─────────────────────────
        measureNumberLabel?.let { mn ->
            val mnPaint = Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.GRAY
                textSize = lineSpacing * 0.65f
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                "$mn",
                staffStartX + lineSpacing * 0.1f,
                trebleTopY - lineSpacing * 0.15f,
                mnPaint
            )
        }

        // ── Chord ghost lines + labels ────────────────────────────────────────
        val labelTextSize = (beatWidth * 0.5f).coerceIn(10f, lineSpacing * 0.42f)
        val labelPaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = labelTextSize
            isAntiAlias = true
        }
        val trebleLabelY = trebleTopY - lineSpacing * 0.55f
        val bassLabelY   = bassTopY   + lineSpacing * 5.3f

        rowChords.forEach { chord ->
            val x = beatX(localBeat(chord.startBeat))
            drawLine(
                color = Color(0x447E8798),
                start = Offset(x, trebleTopY - lineSpacing * 0.6f),
                end = Offset(x, bassTopY + lineSpacing * 4f),
                strokeWidth = 1f
            )
            val label = formatChordLabelShort(chord.notes)
            val labelY = if (chord.staff == StaffType.BASS) bassLabelY else trebleLabelY
            drawContext.canvas.nativeCanvas.drawText(label, x + 2f, labelY, labelPaint)
        }

        // ── Note heads, ledger lines, stems, flags / beams ───────────────────
        val noteHeadWidth = lineSpacing * 1.1f
        val noteHeadHeight = lineSpacing * 0.76f

        val sortedChordMap = rowNotes
            .groupBy { it.startBeat to it.staff }
            .toSortedMap(compareBy<Pair<Float, StaffType>> { it.first }.thenBy { it.second.ordinal })

        // ── Beam pre-computation ──────────────────────────────────────────────
        val beamGroups = computeBeamGroups(rowNotes)
        val beamGroupByBeatStaff: Map<Pair<Float, StaffType>, BeamGroup> = buildMap {
            beamGroups.forEach { group ->
                group.beatPositions.forEach { beat -> put(beat to group.staff, group) }
            }
        }
        // Pass 1: collect natural stem end-Y for each beamed chord position → stemX to naturalEndY
        val beamedNaturalStem = mutableMapOf<Pair<Float, StaffType>, Pair<Float, Float>>()
        sortedChordMap.forEach { (key, chordNotes) ->
            val beamGroup = beamGroupByBeatStaff[key] ?: return@forEach
            if (durationToGlyphType(chordNotes.first().duration) != NoteGlyphType.EIGHTH) return@forEach
            val (beat, staff) = key
            val baseX = beatX(localBeat(beat))
            val sortedNotes = chordNotes.sortedBy { displayDiatonicStep(it.midi, it.accidental) }
            val steps = sortedNotes.map { displayDiatonicStep(it.midi, it.accidental) }
            val xOffsets = chordNoteheadOffsets(steps, beamGroup.direction, lineSpacing)
            val middleY = staffLineYForStep(middleLineStepForStaff(staff), staff, trebleTopY, bassTopY, lineSpacing)
            val noteCentersXY = sortedNotes.mapIndexed { i, note ->
                (baseX + xOffsets[i]) to midiToGrandStaffY(note.midi, note.staff, trebleTopY, bassTopY, lineSpacing, note.accidental)
            }
            val stem = stemGeometryForChord(
                noteXs = noteCentersXY.map { it.first },
                noteYs = noteCentersXY.map { it.second },
                direction = beamGroup.direction,
                middleLineY = middleY,
                lineSpacing = lineSpacing,
                noteHeadWidth = noteHeadWidth
            )
            beamedNaturalStem[key] = stem.x to stem.endY
        }
        // Compute beam Y per group: the Y all stems in the group extend to
        val beamYByGroup = beamGroups.associateWith { group ->
            val endYs = group.beatPositions.mapNotNull { beat -> beamedNaturalStem[beat to group.staff]?.second }
            when (group.direction) {
                StemDirection.UP -> endYs.minOrNull() ?: 0f   // most upward = smallest Y
                StemDirection.DOWN -> endYs.maxOrNull() ?: 0f // most downward = largest Y
            }
        }

        // ── Main rendering loop ───────────────────────────────────────────────
        sortedChordMap.values.forEach { chordNotes ->
                val key = chordNotes.first().startBeat to chordNotes.first().staff
                val baseX = beatX(localBeat(chordNotes.first().startBeat))
                val staff = chordNotes.first().staff
                val glyph = durationToGlyphType(chordNotes.first().duration)
                val beamGroup = beamGroupByBeatStaff[key]
                val sortedNotes = chordNotes.sortedBy { displayDiatonicStep(it.midi, it.accidental) }
                val steps = sortedNotes.map { displayDiatonicStep(it.midi, it.accidental) }
                val direction = beamGroup?.direction ?: stemDirectionForSteps(steps, staff)
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
                            symbol, accidentalX, noteY + lineSpacing * 0.34f, noteAccidentalPaint
                        )
                    }
                }

                if (glyph != NoteGlyphType.WHOLE) {
                    val middleY = staffLineYForStep(
                        middleLineStepForStaff(staff), staff, trebleTopY, bassTopY, lineSpacing
                    )
                    val stem = stemGeometryForChord(
                        noteXs = noteCenters.map { it.second },
                        noteYs = noteCenters.map { it.third },
                        direction = direction,
                        middleLineY = middleY,
                        lineSpacing = lineSpacing,
                        noteHeadWidth = noteHeadWidth
                    )
                    // Extend stem to beam Y if this chord belongs to a beam group
                    val finalStemEndY = if (beamGroup != null && glyph == NoteGlyphType.EIGHTH) {
                        beamYByGroup[beamGroup] ?: stem.endY
                    } else {
                        stem.endY
                    }
                    val stemColor = stemColorForStates(noteCenters.map { it.first.state })
                    drawLine(
                        color = stemColor,
                        start = Offset(stem.x, stem.startY),
                        end = Offset(stem.x, finalStemEndY),
                        strokeWidth = 2f
                    )
                    // Draw flags only for non-beamed notes
                    if (beamGroup == null) {
                        val flagCount = when (glyph) {
                            NoteGlyphType.EIGHTH -> 1
                            NoteGlyphType.SIXTEENTH -> 2
                            else -> 0
                        }
                        repeat(flagCount) { flagIndex ->
                            val flagY = if (direction == StemDirection.UP) {
                                finalStemEndY + lineSpacing * (0.1f + flagIndex * 0.7f)
                            } else {
                                finalStemEndY - lineSpacing * (0.1f + flagIndex * 0.7f)
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
            }

        // ── Beam bars ─────────────────────────────────────────────────────────
        beamGroups.forEach { group ->
            val beamY = beamYByGroup[group] ?: return@forEach
            val firstStemX = beamedNaturalStem[group.beatPositions.first() to group.staff]?.first ?: return@forEach
            val lastStemX = beamedNaturalStem[group.beatPositions.last() to group.staff]?.first ?: return@forEach
            drawLine(
                color = black,
                start = Offset(firstStemX, beamY),
                end = Offset(lastStemX, beamY),
                strokeWidth = lineSpacing * 0.5f
            )
        }

        // ── Pedal marks ───────────────────────────────────────────────────────
        val pedalPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = lineSpacing * 0.85f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val pedalY = bassTopY + lineSpacing * 7.2f
        rowPedals.forEach { pedalMark ->
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
                    beatX(localBeat(pedalMark.startBeat)) + beatWidth * 0.3f,
                    pedalY,
                    pedalPaint
                )
            }
        }

        // ── Red cursor line ───────────────────────────────────────────────────
        val showCursor = if (isPortraitRow) {
            gameState.currentBeat >= startBeat && gameState.currentBeat < endBeat
        } else {
            true
        }
        if (showCursor) {
            val cursorLocal = localBeat(gameState.currentBeat)
            val cursorX = beatX(cursorLocal)
            drawLine(
                color = Color.Red,
                start = Offset(cursorX, trebleTopY - lineSpacing),
                end = Offset(cursorX, bassTopY + lineSpacing * 4.5f),
                strokeWidth = 3f
            )
        }
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

    // Compute cumulative beat positions: each step width depends on its noteValue.
    val stepBeats = run {
        var cursor = 0f
        exercise.steps.map { step ->
            val beat = cursor
            cursor += step.noteValue.uiBeatUnits
            beat
        }
    }
    val currentBeatCumulative = if (exercise.currentIndex < stepBeats.size) {
        stepBeats[exercise.currentIndex]
    } else {
        stepBeats.lastOrNull()?.let { it + exercise.steps.last().noteValue.uiBeatUnits } ?: 0f
    }

    val expectedNotes = exercise.steps.flatMapIndexed { beatIndex, step ->
        val stepBeat = stepBeats[beatIndex]
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
                duration = step.noteValue.beats,
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
                duration = step.noteValue.beats,
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
            startBeat = stepBeats[idx],
            staff = chordStaff
        )
    }

    val pedalMarks = exercise.steps.flatMapIndexed { idx, step ->
        val beat = stepBeats[idx]
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
        currentBeat = currentBeatCumulative,
        musicalKey = exercise.musicalKey
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
