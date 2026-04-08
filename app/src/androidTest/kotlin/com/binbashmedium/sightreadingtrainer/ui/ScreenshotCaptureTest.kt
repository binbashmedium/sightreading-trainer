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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented screenshot test that captures every screen of the app.
 *
 * Uses UiDevice (UiAutomator) for all navigation — more reliable than
 * Compose test's waitUntil across navigation transitions — and
 * UiDevice.takeScreenshot() (PixelCopy API) for captures so that
 * hardware-accelerated WebView surfaces (Verovio WASM) are included.
 *
 * Screens captured:
 *   1. MainScreen              → /sdcard/screenshot_main.png
 *   2. SettingsScreen          → /sdcard/screenshot_settings.png
 *   3. StatisticsScreen        → /sdcard/screenshot_statistics.png
 *   4. HelpScreen              → /sdcard/screenshot_help.png
 *   5. PracticeScreen (staff)  → /sdcard/screenshot_practice.png
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {

    /** Only used to launch MainActivity; all interactions go through UiDevice. */
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val NAV_TIMEOUT_MS = 20_000L

    @Test
    fun captureAllScreens() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext.applicationContext
        val device = UiDevice.getInstance(instrumentation)

        // Configure DataStore with max settings so practice renders
        // the most complex exercise content.
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
        device.wait(Until.hasObject(By.text("Start Practice")), NAV_TIMEOUT_MS)
        Thread.sleep(500)
        device.takeScreenshot(File("/sdcard/screenshot_main.png"), 1.0f, 100)

        // ── 2. SettingsScreen ────────────────────────────────────────────────
        device.findObject(By.text("Settings")).click()
        device.wait(Until.hasObject(By.text("Back")), NAV_TIMEOUT_MS)
        Thread.sleep(500)
        device.takeScreenshot(File("/sdcard/screenshot_settings.png"), 1.0f, 100)
        // Use system back — more reliable than finding the "Back" button node
        device.pressBack()

        // ── 3. StatisticsScreen ──────────────────────────────────────────────
        device.wait(Until.hasObject(By.text("Statistics")), NAV_TIMEOUT_MS)
        device.findObject(By.text("Statistics")).click()
        device.wait(Until.hasObject(By.text("Back")), NAV_TIMEOUT_MS)
        Thread.sleep(500)
        device.takeScreenshot(File("/sdcard/screenshot_statistics.png"), 1.0f, 100)
        device.pressBack()

        // ── 4. HelpScreen ────────────────────────────────────────────────────
        device.wait(Until.hasObject(By.text("Help")), NAV_TIMEOUT_MS)
        device.findObject(By.text("Help")).click()
        device.wait(Until.hasObject(By.text("Back")), NAV_TIMEOUT_MS)
        Thread.sleep(500)
        device.takeScreenshot(File("/sdcard/screenshot_help.png"), 1.0f, 100)
        device.pressBack()

        // ── 5. PracticeScreen ────────────────────────────────────────────────
        device.wait(Until.hasObject(By.text("Start Practice")), NAV_TIMEOUT_MS)
        device.findObject(By.text("Start Practice")).click()

        // Wait for practice screen header ("New Exercise" button)
        device.wait(Until.hasObject(By.text("New Exercise")), NAV_TIMEOUT_MS)

        // Reset render signal then trigger exercise generation
        VerovioRenderSignal.rendered = false
        device.findObject(By.text("New Exercise")).click()

        // Wait for Verovio WASM to JIT-compile and render SVG.
        // On a cold emulator the 6.7 MB WASM can take 30–60 s; allow 120 s.
        val deadline = System.currentTimeMillis() + 120_000L
        while (!VerovioRenderSignal.rendered && System.currentTimeMillis() < deadline) {
            Thread.sleep(2_000)
        }

        // Extra half-second for SVG layout/paint to propagate to display.
        Thread.sleep(500)
        device.takeScreenshot(File("/sdcard/screenshot_practice.png"), 1.0f, 100)

        // Ensure all files are flushed before test exits.
        Thread.sleep(500)
    }
}
