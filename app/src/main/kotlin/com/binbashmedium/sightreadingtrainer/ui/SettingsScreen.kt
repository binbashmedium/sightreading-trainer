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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.binbashmedium.sightreadingtrainer.R
import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.ChordProgression
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val devices by viewModel.availableMidiDevices.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))

        // Difficulty (1–5)
        Text("Exercise Types")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExerciseContentType.entries.forEach { type ->
                FilterChip(
                    selected = type in settings.exerciseTypes,
                    onClick = {
                        val updatedTypes = settings.exerciseTypes.toMutableSet().apply {
                            if (contains(type)) {
                                if (size > 1) remove(type)
                            } else {
                                add(type)
                            }
                        }
                        viewModel.updateSettings(settings.copy(exerciseTypes = updatedTypes))
                    },
                    label = { Text(type.name.replace('_', ' ')) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Exercise Time: ${settings.exerciseTimeSec} s")
        Slider(
            value = settings.exerciseTimeSec.toFloat(),
            onValueChange = { viewModel.updateSettings(settings.copy(exerciseTimeSec = it.toInt())) },
            valueRange = 30f..300f,
            steps = 26
        )

        // Progression selector — shown only when PROGRESSIONS type is active
        if (ExerciseContentType.PROGRESSIONS in settings.exerciseTypes) {
            Spacer(Modifier.height(16.dp))
            Text("Chord Progressions")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChordProgression.entries.forEach { prog ->
                    FilterChip(
                        selected = prog in settings.selectedProgressions,
                        onClick = {
                            val updated = settings.selectedProgressions.toMutableSet().apply {
                                if (contains(prog)) {
                                    if (size > 1) remove(prog)
                                } else {
                                    add(prog)
                                }
                            }
                            viewModel.updateSettings(settings.copy(selectedProgressions = updated))
                        },
                        label = { Text(prog.displayName) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Key signature pool
        Text("Keys: ${settings.selectedKeys.sorted().joinToString(", ") { KEY_NAMES.getOrElse(it) { "C" } }}")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(KEY_NAMES) { index, keyName ->
                FilterChip(
                    selected = index in settings.selectedKeys,
                    onClick = {
                        val updatedKeys = settings.selectedKeys.toMutableSet().apply {
                            if (contains(index)) {
                                if (size > 1) remove(index)
                            } else {
                                add(index)
                            }
                        }
                        viewModel.updateSettings(settings.copy(selectedKeys = updatedKeys))
                    },
                    label = { Text(keyName) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Hand mode
        Text(stringResource(R.string.hand_mode))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HandMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.handMode == mode,
                    onClick = { viewModel.updateSettings(settings.copy(handMode = mode)) },
                    label = { Text(mode.name) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generated Note Accidentals")
            Switch(
                checked = settings.noteAccidentalsEnabled,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(noteAccidentalsEnabled = it))
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generated Pedal Marks")
            Switch(
                checked = settings.pedalEventsEnabled,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(pedalEventsEnabled = it))
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Timing tolerance
        Text("${stringResource(R.string.timing_tolerance)}: ${settings.timingToleranceMs} ms")
        Slider(
            value = settings.timingToleranceMs.toFloat(),
            onValueChange = { viewModel.updateSettings(settings.copy(timingToleranceMs = it.toInt())) },
            valueRange = 50f..500f
        )

        Spacer(Modifier.height(16.dp))

        // Chord detection window
        Text("${stringResource(R.string.chord_window)}: ${settings.chordWindowMs} ms")
        Slider(
            value = settings.chordWindowMs.toFloat(),
            onValueChange = { viewModel.updateSettings(settings.copy(chordWindowMs = it.toInt())) },
            valueRange = 20f..200f
        )

        Spacer(Modifier.height(16.dp))

        // Sound toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.sound_enabled))
            Switch(
                checked = settings.soundEnabled,
                onCheckedChange = { viewModel.updateSettings(settings.copy(soundEnabled = it)) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // MIDI device selector
        if (devices.isNotEmpty()) {
            Text(stringResource(R.string.midi_device))
            devices.forEach { deviceName ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = settings.midiDeviceName == deviceName,
                        onClick = { viewModel.updateSettings(settings.copy(midiDeviceName = deviceName)) }
                    )
                    Text(deviceName)
                }
            }
        } else {
            Text("No MIDI devices connected", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Back") }
    }
}
