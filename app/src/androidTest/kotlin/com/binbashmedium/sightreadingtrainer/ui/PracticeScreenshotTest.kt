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
        composeTestRule
            .onNodeWithText("New Exercise")
            .performClick()

        // Allow the Grand Staff Canvas to complete its first render pass
        Thread.sleep(4_000)

        // Capture the full screen while the app is still in the foreground.
        // Using executeShellCommand so the screenshot is taken synchronously
        // before the test runner can navigate away.
        val device = UiDevice.getInstance(instrumentation)
        device.executeShellCommand("screencap -p /sdcard/practice_screen.png")

        // Brief pause to ensure the screencap command completes before the test ends
        Thread.sleep(500)
    }
}
