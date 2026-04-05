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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented screenshot test for PracticeScreen.
 *
 * Requires a running emulator or device. Navigates to PracticeScreen, waits for the
 * Verovio WebView to finish rendering, then captures the full screen (including
 * hardware-accelerated surfaces) using UiDevice.takeScreenshot().
 *
 * Output: <app-external-files-dir>/practice_screen.png
 * CI pulls this file via:
 *   adb pull /sdcard/Android/data/com.binbashmedium.sightreadingtrainer/files/practice_screen.png
 */
@RunWith(AndroidJUnit4::class)
class PracticeScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun capturePracticeScreen() {
        // Navigate from MainScreen to PracticeScreen
        composeTestRule
            .onNodeWithText("Start Practice")
            .performClick()

        // Wait until the practice screen is ready (New Exercise button rendered)
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule
                .onAllNodesWithText("New Exercise")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Give the Verovio WebView time to complete its first render
        Thread.sleep(6_000)

        // Capture the full screen including WebView hardware layer via PixelCopy
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outFile = File(context.getExternalFilesDir(null), "practice_screen.png")
        device.takeScreenshot(outFile, 1.0f, 90)
    }
}
