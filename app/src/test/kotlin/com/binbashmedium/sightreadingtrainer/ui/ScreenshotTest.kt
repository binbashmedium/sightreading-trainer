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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    private fun <T> snap(name: String? = null, content: @androidx.compose.runtime.Composable () -> T) {
        paparazzi.snapshot(name = name) {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }

    // ── Main Screen ──────────────────────────────────────────────────────────

    @Test
    fun mainScreen_default() = snap("main_default") {
        MainScreenContent(settings = AppSettings())
    }

    @Test
    fun mainScreen_withStats() = snap("main_with_stats") {
        MainScreenContent(
            settings = AppSettings(
                exerciseTimeMin = 5,
                exerciseTypes = setOf(ExerciseContentType.TRIADS, ExerciseContentType.PROGRESSIONS),
                selectedKeys = setOf(0, 2, 5, 7),
                highScore = 1240,
                totalCorrectNotes = 320,
                totalWrongNotes = 48
            )
        )
    }

    // ── Settings Screen ──────────────────────────────────────────────────────

    @Test
    fun settingsScreen_default() = snap("settings_default") {
        SettingsScreenContent(settings = AppSettings())
    }

    @Test
    fun settingsScreen_withMidiDevice() = snap("settings_midi_device") {
        SettingsScreenContent(
            settings = AppSettings(midiDeviceName = "USB MIDI Keyboard"),
            availableDevices = listOf("USB MIDI Keyboard", "Bluetooth Piano")
        )
    }

    @Test
    fun settingsScreen_withProgressions() = snap("settings_progressions") {
        SettingsScreenContent(
            settings = AppSettings(
                exerciseTypes = setOf(
                    ExerciseContentType.SINGLE_NOTES,
                    ExerciseContentType.PROGRESSIONS
                )
            )
        )
    }

    // ── Statistics Screen ────────────────────────────────────────────────────

    @Test
    fun statisticsScreen_empty() = snap("statistics_empty") {
        StatisticsScreenContent(state = StatisticsUiState())
    }

    @Test
    fun statisticsScreen_withData() = snap("statistics_with_data") {
        StatisticsScreenContent(
            state = StatisticsUiState(
                topCorrectGroups = listOf(
                    StatItemUi("C E G", 42),
                    StatItemUi("G B D", 35),
                    StatItemUi("F A C", 28),
                    StatItemUi("D F# A", 21),
                    StatItemUi("A C# E", 14)
                ),
                topWrongGroups = listOf(
                    StatItemUi("E G# B", 18),
                    StatItemUi("Bb D F", 12),
                    StatItemUi("C# F G#", 9)
                ),
                topCorrectNotes = listOf(
                    StatItemUi("C4", 85),
                    StatItemUi("G4", 72),
                    StatItemUi("E4", 68),
                    StatItemUi("D4", 55),
                    StatItemUi("A4", 49)
                ),
                topWrongNotes = listOf(
                    StatItemUi("F#4", 22),
                    StatItemUi("Bb3", 17),
                    StatItemUi("C#5", 11)
                )
            )
        )
    }

    // ── Help Screen ──────────────────────────────────────────────────────────

    @Test
    fun helpScreen() = snap("help") {
        HelpScreenContent()
    }
}
