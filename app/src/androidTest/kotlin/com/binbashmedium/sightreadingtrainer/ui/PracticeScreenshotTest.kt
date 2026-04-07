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

/**
 * Instrumented screenshot test for PracticeScreen.
 *
 * Requires a running emulator or device. Configures maximum exercise settings
 * (all content types, all keys, both hands, pedal marks, chord names, all ornaments),
 * navigates to PracticeScreen, waits for the Grand Staff Canvas to finish rendering,
 * then captures the full screen via a shell screencap command so the screenshot is
 * taken while the app is still in the foreground.
 *
 * Output saved to /sdcard/practice_screen.png on the device.
 * CI pulls this file via:
 *   adb pull /sdcard/practice_screen.png practice_screen.png
 */
@RunWith(AndroidJUnit4::class)
class PracticeScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun capturePracticeScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext.applicationContext

        // Write maximum configuration to DataStore before generating any exercise.
        // The SettingsDataStore singleton is already bound in the Hilt component;
        // we access it here via an @EntryPoint to avoid requiring @HiltAndroidTest.
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

        // Navigate from MainScreen to PracticeScreen
        composeTestRule
            .onNodeWithText("Start Practice")
            .performClick()

        // Wait until the practice screen is ready
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule
                .onAllNodesWithText("New Exercise")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Regenerate the exercise so it uses the max config written above.
        // Reset the render signal BEFORE triggering the new exercise so we don't
        // accidentally read a stale 'true' from any earlier render.
        VerovioRenderSignal.rendered = false
        composeTestRule
            .onNodeWithText("New Exercise")
            .performClick()

        // Wait until staff.html calls Android.onRendered() — i.e., the Verovio SVG is actually
        // in the DOM. On a cold emulator the 6.7 MB WASM can take 30–60 s to JIT-compile, so
        // a fixed sleep is unreliable. Allow up to 120 s before giving up.
        val deadline = System.currentTimeMillis() + 120_000L
        while (!VerovioRenderSignal.rendered && System.currentTimeMillis() < deadline) {
            Thread.sleep(2_000)
        }

        // Extra half-second for the SVG layout/paint to propagate to the display.
        Thread.sleep(500)

        // Capture the full screen while the app is still in the foreground.
        val device = UiDevice.getInstance(instrumentation)
        device.executeShellCommand("screencap -p /sdcard/practice_screen.png")

        // Brief pause to ensure the screencap file is fully written before the test exits.
        Thread.sleep(500)
    }
}
