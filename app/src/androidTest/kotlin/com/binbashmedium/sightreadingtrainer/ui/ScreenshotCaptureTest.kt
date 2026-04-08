package com.binbashmedium.sightreadingtrainer.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.binbashmedium.sightreadingtrainer.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {

    @Test
    fun practiceScreenRendersWithoutSystemAnrDialog() {
        ActivityScenario.launch(MainActivity::class.java).use {
            dismissSystemAnrDialogs()
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

            device.wait(Until.hasObject(By.textContains("New Exercise")), 10_000)
            device.findObject(By.textContains("New Exercise"))?.click()

            assertTrue(waitForVerovioRender(timeoutMs = 20_000))
            dismissSystemAnrDialogs()
        }
    }

    private fun waitForVerovioRender(timeoutMs: Long): Boolean {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            dismissSystemAnrDialogs()
            if (device.hasObject(By.descContains("svg")) || device.hasObject(By.textContains("Practice"))) {
                return true
            }
            Thread.sleep(250)
        }
        return false
    }

    private fun dismissSystemAnrDialogs() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        listOf("Wait", "Close app", "OK", "Schließen", "Warten", "App schließen")
            .forEach { label -> device.findObject(By.text(label))?.click() }

        waitUntilGone(By.textContains("isn't responding"), 1_000)
    }

    private fun waitUntilGone(selector: BySelector, timeoutMs: Long): Boolean {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (!device.hasObject(selector)) {
                return true
            }
            Thread.sleep(100)
        }
        return !device.hasObject(selector)
    }
}
