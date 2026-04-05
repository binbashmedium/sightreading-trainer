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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.binbashmedium.sightreadingtrainer.R

@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val typeSummary = formatExerciseTypeSummary(settings.exerciseTypes)
    val keySummary = settings.selectedKeys.sorted().joinToString(", ") { KEY_NAMES[it] }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(16.dp))

        Text("Time: ${settings.exerciseTimeMin} min")
        Text(
            text = "Types: $typeSummary",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Keys: $keySummary",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text("Hand: ${settings.handMode.name}")
        Text("Highscore: ${settings.highScore} pts")
        Text("Correct / Wrong: ${settings.totalCorrectNotes} / ${settings.totalWrongNotes}")

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("practice") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.start_practice))
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { navController.navigate("settings") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings))
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { navController.navigate("statistics") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Statistics")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { navController.navigate("help") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Help")
        }

        Spacer(Modifier.height(12.dp))
    }
}

internal fun formatExerciseTypeSummary(types: Set<com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType>): String {
    if (types.isEmpty()) return "SINGLE NOTES"
    val names = types
        .sortedBy { it.ordinal }
        .map { it.name.replace('_', ' ') }

    return if (names.size <= 3) {
        names.joinToString(", ")
    } else {
        "${names.take(3).joinToString(", ")} +${names.size - 3} more"
    }
}
