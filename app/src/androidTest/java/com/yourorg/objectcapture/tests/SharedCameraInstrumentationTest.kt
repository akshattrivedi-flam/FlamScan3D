package com.yourorg.objectcapture.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yourorg.objectcapture.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Basic instrumentation harness that launches the main activity.
 * Intended for on-device validation of ARCore SharedCamera + CameraX alignment.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SharedCameraInstrumentationTest {
    @Test
    fun launchMainActivity() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // If launch succeeds, the test passes. Additional device checks should be manual.
        }
    }
}
