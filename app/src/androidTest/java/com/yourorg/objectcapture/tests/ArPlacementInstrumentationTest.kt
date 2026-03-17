package com.yourorg.objectcapture.tests

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yourorg.objectcapture.ArPlacementActivity
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Launches the AR Placement activity. Use on-device to validate hit testing + background compositing.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ArPlacementInstrumentationTest {
    @Test
    fun launchArPlacement() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, ArPlacementActivity::class.java)
        ActivityScenario.launch<ArPlacementActivity>(intent).use {
            // Manual validation on device is required for placement and rendering quality.
        }
    }
}
