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
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseInputSource
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseMode
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteValue
import com.binbashmedium.sightreadingtrainer.domain.model.OrnamentType
import com.binbashmedium.sightreadingtrainer.domain.model.ProgressionExerciseType
import com.binbashmedium.sightreadingtrainer.domain.model.ScaleType

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

        Text("Exercise Mode")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExerciseMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.exerciseMode == mode,
                    onClick = { viewModel.updateSettings(settings.copy(exerciseMode = mode)) },
                    label = { Text(if (mode == ExerciseMode.CLASSIC) "Mode 1" else "Mode 2") }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(if (settings.exerciseMode == ExerciseMode.CLASSIC) "Exercise Types (Mode 1)" else "Progression Voicings (Mode 2)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (settings.exerciseMode == ExerciseMode.CLASSIC) {
                ExerciseContentType.entries
                    .filter { it != ExerciseContentType.PROGRESSIONS }
                    .forEach { type ->
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
            } else {
                ProgressionExerciseType.entries.forEach { type ->
                    FilterChip(
                        selected = type in settings.progressionExerciseTypes,
                        onClick = {
                            val updatedTypes = settings.progressionExerciseTypes.toMutableSet().apply {
                                if (contains(type)) {
                                    if (size > 1) remove(type)
                                } else {
                                    add(type)
                                }
                            }
                            viewModel.updateSettings(settings.copy(progressionExerciseTypes = updatedTypes))
                        },
                        label = { Text(type.name.replace('_', ' ')) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Exercise Input Source")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExerciseInputSource.entries.forEach { source ->
                FilterChip(
                    selected = settings.exerciseInputSource == source,
                    onClick = { viewModel.updateSettings(settings.copy(exerciseInputSource = source)) },
                    label = { Text(source.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Exercise Time: ${settings.exerciseTimeMin} min")
        Slider(
            value = settings.exerciseTimeMin.toFloat(),
            onValueChange = { viewModel.updateSettings(settings.copy(exerciseTimeMin = it.toInt())) },
            valueRange = 1f..10f,
            steps = 8
        )

        // Progression selector — shown only when PROGRESSIONS type is active
        if (settings.exerciseMode == ExerciseMode.PROGRESSIONS) {
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

        Spacer(Modifier.height(12.dp))

        Text("Skala")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScaleType.entries.forEach { scaleType ->
                FilterChip(
                    selected = settings.selectedScaleType == scaleType,
                    onClick = { viewModel.updateSettings(settings.copy(selectedScaleType = scaleType)) },
                    label = { Text(scaleType.displayName) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Note value pool (whole / half / quarter / eighth)
        Text("Note Values")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoteValue.entries.forEach { nv ->
                FilterChip(
                    selected = nv in settings.selectedNoteValues,
                    onClick = {
                        val updated = settings.selectedNoteValues.toMutableSet().apply {
                            if (contains(nv)) {
                                if (size > 1) remove(nv)
                            } else {
                                add(nv)
                            }
                        }
                        viewModel.updateSettings(settings.copy(selectedNoteValues = updated))
                    },
                    label = { Text(nv.name.lowercase().replaceFirstChar { it.uppercase() }) }
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
            Text("Show Chord Names")
            Switch(
                checked = settings.chordNamesEnabled,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(chordNamesEnabled = it))
                }
            )
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

        Text("Ornaments", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(listOf(
                OrnamentType.TRILL,
                OrnamentType.MORDENT,
                OrnamentType.UPPER_MORDENT,
                OrnamentType.TURN,
                OrnamentType.APPOGGIATURA,
                OrnamentType.ACCIACCATURA,
                OrnamentType.ARPEGGIATION
            )) { _, type ->
                FilterChip(
                    selected = type in settings.selectedOrnaments,
                    onClick = {
                        val updated = if (type in settings.selectedOrnaments)
                            settings.selectedOrnaments - type
                        else
                            settings.selectedOrnaments + type
                        viewModel.updateSettings(settings.copy(selectedOrnaments = updated))
                    },
                    label = {
                        Text(when (type) {
                            OrnamentType.UPPER_MORDENT -> "Upper Mordent"
                            OrnamentType.MORDENT       -> "Lower Mordent"
                            OrnamentType.ACCIACCATURA  -> "Acciaccatura"
                            OrnamentType.APPOGGIATURA  -> "Appoggiatura"
                            OrnamentType.ARPEGGIATION  -> "Arpeggiation"
                            else -> type.name.lowercase().replaceFirstChar { it.uppercase() }
                        })
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Note ranges
        Text("Bass Range: MIDI ${settings.bassNoteRangeMin}–${settings.bassNoteRangeMax}")
        Text("Min (E1=28)", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = settings.bassNoteRangeMin.toFloat(),
            onValueChange = {
                val newMin = it.toInt().coerceAtMost(settings.bassNoteRangeMax - 12)
                viewModel.updateSettings(settings.copy(bassNoteRangeMin = newMin))
            },
            valueRange = 28f..72f,
            steps = 43
        )
        Text("Max (C5=72)", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = settings.bassNoteRangeMax.toFloat(),
            onValueChange = {
                val newMax = it.toInt().coerceAtLeast(settings.bassNoteRangeMin + 12)
                viewModel.updateSettings(settings.copy(bassNoteRangeMax = newMax))
            },
            valueRange = 28f..72f,
            steps = 43
        )

        Spacer(Modifier.height(8.dp))

        Text("Treble Range: MIDI ${settings.trebleNoteRangeMin}–${settings.trebleNoteRangeMax}")
        Text("Min (C3=48)", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = settings.trebleNoteRangeMin.toFloat(),
            onValueChange = {
                val newMin = it.toInt().coerceAtMost(settings.trebleNoteRangeMax - 12)
                viewModel.updateSettings(settings.copy(trebleNoteRangeMin = newMin))
            },
            valueRange = 48f..93f,
            steps = 44
        )
        Text("Max (A6=93)", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = settings.trebleNoteRangeMax.toFloat(),
            onValueChange = {
                val newMax = it.toInt().coerceAtLeast(settings.trebleNoteRangeMin + 12)
                viewModel.updateSettings(settings.copy(trebleNoteRangeMax = newMax))
            },
            valueRange = 48f..93f,
            steps = 44
        )

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
