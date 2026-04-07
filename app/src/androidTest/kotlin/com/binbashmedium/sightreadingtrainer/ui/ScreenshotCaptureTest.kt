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

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.binbashmedium.sightreadingtrainer.MainActivity
import com.binbashmedium.sightreadingtrainer.di.SettingsDataStoreEntryPoint
import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.ChordProgression
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseInputSource
import com.binbashmedium.sightreadingtrainer.domain.model.NoteValue
import com.binbashmedium.sightreadingtrainer.domain.model.OrnamentType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented screenshot test that captures every screen of the app.
 *
 * Uses UiDevice.takeScreenshot() (PixelCopy API) instead of `adb screencap`
 * so hardware-accelerated WebView surfaces (Verovio WASM rendering) are
 * included correctly in the captured image.
 *
 * Screens captured:
 *   1. MainScreen              → /sdcard/screenshot_main.png
 *   2. SettingsScreen          → /sdcard/screenshot_settings.png
 *   3. StatisticsScreen        → /sdcard/screenshot_statistics.png
 *   4. HelpScreen              → /sdcard/screenshot_help.png
 *   5. PracticeScreen (staff)  → /sdcard/screenshot_practice.png
 *
 * CI pulls these files via:
 *   adb pull /sdcard/screenshot_main.png
 *   adb pull /sdcard/screenshot_settings.png
 *   ... etc.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureAllScreens() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext.applicationContext
        val device = UiDevice.getInstance(instrumentation)

        // Write maximum configuration to DataStore so the practice screen
        // renders with the most complex exercise content.
        val settingsDataStore = EntryPointAccessors.fromApplication(
            appContext,
            SettingsDataStoreEntryPoint::class.java
        ).settingsDataStore()

        val maxConfig = AppSettings(
            exerciseTypes = ExerciseContentType.entries.toSet(),
            exerciseInputSource = ExerciseInputSource.GENERATED,
            handMode = HandMode.BOTH,
            noteAccidentalsEnabled = true,
            pedalEventsEnabled = true,
            chordNamesEnabled = true,
            selectedKeys = (0..11).toSet(),
            selectedProgressions = ChordProgression.entries.toSet(),
            selectedNoteValues = NoteValue.entries.toSet(),
            selectedOrnaments = OrnamentType.entries.filter { it != OrnamentType.NONE }.toSet()
        )
        runBlocking { settingsDataStore.updateSettings(maxConfig) }

        // ── 1. MainScreen ────────────────────────────────────────────────────
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Start Practice")
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(300)
        takeScreenshot(device, "/sdcard/screenshot_main.png")

        // ── 2. SettingsScreen ────────────────────────────────────────────────
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Back")
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(300)
        takeScreenshot(device, "/sdcard/screenshot_settings.png")
        composeTestRule.onNodeWithText("Back").performClick()

        // ── 3. StatisticsScreen ──────────────────────────────────────────────
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Start Practice")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Statistics").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Back")
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(300)
        takeScreenshot(device, "/sdcard/screenshot_statistics.png")
        composeTestRule.onNodeWithText("Back").performClick()

        // ── 4. HelpScreen ────────────────────────────────────────────────────
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Start Practice")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Help").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Back")
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(300)
        takeScreenshot(device, "/sdcard/screenshot_help.png")
        composeTestRule.onNodeWithText("Back").performClick()

        // ── 5. PracticeScreen ────────────────────────────────────────────────
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Start Practice")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Start Practice").performClick()

        // Wait for practice screen to be ready
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithText("New Exercise")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Reset render signal before triggering new exercise
        VerovioRenderSignal.rendered = false
        composeTestRule.onNodeWithText("New Exercise").performClick()

        // Wait for Verovio WASM to JIT-compile and render the SVG.
        // On a cold emulator the 6.7 MB WASM can take 30–60 s; allow up to 120 s.
        val deadline = System.currentTimeMillis() + 120_000L
        while (!VerovioRenderSignal.rendered && System.currentTimeMillis() < deadline) {
            Thread.sleep(2_000)
        }

        // Extra half-second for SVG layout/paint to propagate to display.
        Thread.sleep(500)

        takeScreenshot(device, "/sdcard/screenshot_practice.png")

        // Brief pause to ensure all files are fully written before test exits.
        Thread.sleep(500)
    }

    /**
     * Captures the current screen using UiDevice.takeScreenshot() which uses
     * the PixelCopy API — correctly capturing hardware-accelerated surfaces
     * (including WebView GPU compositing layers used by Verovio).
     */
    private fun takeScreenshot(device: UiDevice, path: String) {
        device.takeScreenshot(File(path), 1.0f, 100)
    }
}
