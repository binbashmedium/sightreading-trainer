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
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseInputSource
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteValue
import com.binbashmedium.sightreadingtrainer.domain.model.OrnamentType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Five independent instrumented screenshot tests — one per screen.
 *
 * Design rationale
 * ────────────────
 * Previous single-method approaches failed for two separate reasons:
 *
 *  1. `device.takeScreenshot(File("/sdcard/..."))` silently returns false on
 *     API 34 — the app UID cannot write to /sdcard/ root under scoped storage.
 *     Fix: use `uiAutomation.takeScreenshot()` (PixelCopy — also captures
 *     WebView GPU surfaces) and write to `getExternalFilesDir()` which the app
 *     can always write to without any extra permission.
 *
 *  2. Back navigation (`device.pressBack()` or clicking "Back") combined with
 *     subsequent waitUntil / device.wait timed out on API 34 due to the
 *     predictive-back animation leaving the accessibility tree in a partial
 *     state.  Fix: split into five independent tests so no back navigation is
 *     ever needed — each test starts fresh from MainActivity.
 *
 * Screenshot paths (pulled by CI):
 *   /sdcard/Android/data/com.binbashmedium.sightreadingtrainer/files/screenshot_*.png
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    private val appContext get() = instrumentation.targetContext.applicationContext

    // ── DataStore setup ──────────────────────────────────────────────────────

    /**
     * Write max configuration before every test so all screens reflect the
     * richest possible settings (all exercise types, all keys, both hands,
     * all ornaments, pedal, chord names).  DataStore is file-backed and
     * persists across the test methods in this run.
     */
    @Before
    fun writeMaxConfig() {
        val settingsDataStore = EntryPointAccessors
            .fromApplication(appContext, SettingsDataStoreEntryPoint::class.java)
            .settingsDataStore()
        runBlocking {
            settingsDataStore.updateSettings(
                AppSettings(
                    exerciseTypes       = ExerciseContentType.entries.toSet(),
                    exerciseInputSource = ExerciseInputSource.GENERATED,
                    handMode            = HandMode.BOTH,
                    noteAccidentalsEnabled = true,
                    pedalEventsEnabled     = true,
                    chordNamesEnabled      = true,
                    selectedKeys           = (0..11).toSet(),
                    selectedProgressions   = ChordProgression.entries.toSet(),
                    selectedNoteValues     = NoteValue.entries.toSet(),
                    selectedOrnaments      = OrnamentType.entries
                        .filter { it != OrnamentType.NONE }.toSet()
                )
            )
        }
    }

    // ── Test methods (one per screen, alphabetical = execution order) ────────

    @Test
    fun screenshot01_main() {
        waitFor("Start Practice")
        Thread.sleep(500)
        captureScreen("screenshot_main.png")
    }

    @Test
    fun screenshot02_settings() {
        waitFor("Start Practice")           // confirm we're on MainScreen
        composeTestRule.onNodeWithText("Settings").performClick()
        waitFor("Back")                     // Settings screen ready
        Thread.sleep(500)
        captureScreen("screenshot_settings.png")
    }

    @Test
    fun screenshot03_statistics() {
        waitFor("Statistics")               // button on MainScreen
        composeTestRule.onNodeWithText("Statistics").performClick()
        waitFor("Back")                     // Statistics screen ready
        Thread.sleep(500)
        captureScreen("screenshot_statistics.png")
    }

    @Test
    fun screenshot04_help() {
        waitFor("Help")                     // button on MainScreen
        composeTestRule.onNodeWithText("Help").performClick()
        waitFor("Back")                     // Help screen ready
        Thread.sleep(500)
        captureScreen("screenshot_help.png")
    }

    @Test
    fun screenshot05_practice() {
        // Rotate to landscape first: portrait spawns 4 simultaneous WebViews
        // (one per staff row) each loading the 6.7 MB Verovio WASM bundle —
        // four concurrent WASM JIT compilations on a 2-core CI emulator causes
        // OOM and crashes the process.  Landscape uses only 1 WebView.
        device.setOrientationLandscape()
        try {
            waitFor("Start Practice")
            composeTestRule.onNodeWithText("Start Practice").performClick()

            // Wait for the practice header to appear ("New Exercise" button)
            waitFor("New Exercise", timeoutMs = 20_000)

            // Brief pause for GPU compositing to settle, then capture the
            // initial practice state (empty staff + "New Exercise" button).
            // We intentionally do NOT click "New Exercise" here: triggering a
            // full exercise render would start WASM execution which is likely to
            // OOM on the 2-core CI emulator even in landscape mode.
            Thread.sleep(2_000)
            captureScreen("screenshot_practice.png")
        } finally {
            device.setOrientationNatural()
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun waitFor(text: String, timeoutMs: Long = 15_000) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMs) {
            composeTestRule.onAllNodesWithText(text)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Captures the full screen using UiAutomation.takeScreenshot() (PixelCopy),
     * which correctly captures hardware-accelerated surfaces including the
     * Verovio WebView GPU layer.
     *
     * Writes to getExternalFilesDir() which the app UID can always write to on
     * API 29+ without WRITE_EXTERNAL_STORAGE — unlike /sdcard/ root which is
     * blocked by scoped storage enforcement on API 34.
     *
     * Pull path in CI:
     *   /sdcard/Android/data/com.binbashmedium.sightreadingtrainer/files/<name>
     */
    private fun captureScreen(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        try {
            val dir = appContext.getExternalFilesDir(null) ?: return
            File(dir, name).outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
        } finally {
            bitmap.recycle()
        }
    }
}
