package com.binbashmedium.sightreadingtrainer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.binbashmedium.sightreadingtrainer.R
import com.binbashmedium.sightreadingtrainer.domain.model.MatchResult
import com.binbashmedium.sightreadingtrainer.core.util.NoteNames

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

    val feedbackColor by animateColorAsState(
        targetValue = when (state?.lastResult) {
            is MatchResult.Correct -> Color(0xFF4CAF50)
            is MatchResult.Incorrect -> Color(0xFFF44336)
            is MatchResult.TooEarly -> Color(0xFFFF9800)
            is MatchResult.TooLate -> Color(0xFFFF9800)
            else -> Color.Transparent
        },
        label = "feedbackColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${stringResource(R.string.score)}: ${state?.score ?: 0}/${state?.totalAttempts ?: 0}")
            TextButton(onClick = {
                viewModel.stopSession()
                navController.popBackStack()
            }) { Text("Done") }
        }

        Spacer(Modifier.height(16.dp))

        // Staff display (5 lines)
        StaffView(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            currentChord = state?.exercise?.currentChord
        )

        Spacer(Modifier.height(16.dp))

        // Feedback indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(feedbackColor),
            contentAlignment = Alignment.Center
        ) {
            val feedbackText = when (state?.lastResult) {
                is MatchResult.Correct -> stringResource(R.string.correct)
                is MatchResult.Incorrect -> stringResource(R.string.incorrect)
                is MatchResult.TooEarly -> stringResource(R.string.too_early)
                is MatchResult.TooLate -> stringResource(R.string.too_late)
                else -> ""
            }
            if (feedbackText.isNotEmpty()) {
                Text(feedbackText, color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Piano keyboard
        PianoKeyboard(modifier = Modifier.fillMaxWidth().height(120.dp))

        // Exercise complete banner
        if (state?.exercise?.isComplete == true) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.exercise_complete),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("Score: ${state!!.score} / ${state!!.totalAttempts}")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        viewModel.startSession(mainViewModel.createExercise())
                    }) { Text("New Exercise") }
                }
            }
        }
    }
}

/** Draws a 5-line musical staff with whole notes for the current chord. */
@Composable
fun StaffView(modifier: Modifier = Modifier, currentChord: List<Int>?) {
    Canvas(modifier = modifier) {
        val lineSpacing = size.height / 6f
        val staffTop = lineSpacing
        val lineColor = Color.Black

        // Draw 5 staff lines
        for (i in 0..4) {
            val y = staffTop + i * lineSpacing
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
        }

        // Draw note heads for the current chord
        currentChord?.forEach { midiNote ->
            val noteY = staffTop + (72 - midiNote) * (lineSpacing / 2f)
            drawCircle(
                color = Color.Black,
                radius = lineSpacing * 0.35f,
                center = Offset(size.width / 2f, noteY)
            )
        }
    }
}

/** Simple piano keyboard showing 2 octaves (C4–B5). */
@Composable
fun PianoKeyboard(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val totalWhiteKeys = 14  // 2 octaves
        val keyWidth = size.width / totalWhiteKeys
        val whiteKeyNotes = listOf(60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83)

        // White keys
        whiteKeyNotes.forEachIndexed { index, _ ->
            val x = index * keyWidth
            drawRect(Color.White, topLeft = Offset(x + 1f, 0f),
                size = androidx.compose.ui.geometry.Size(keyWidth - 2f, size.height))
            drawRect(Color.Black, topLeft = Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(keyWidth, size.height),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
        }

        // Black keys
        val blackKeyOffsets = listOf(0.6f, 1.6f, 3.6f, 4.6f, 5.6f, 7.6f, 8.6f, 10.6f, 11.6f, 12.6f)
        blackKeyOffsets.forEach { offset ->
            val x = offset * keyWidth
            drawRect(Color.Black, topLeft = Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(keyWidth * 0.6f, size.height * 0.6f))
        }
    }
}
