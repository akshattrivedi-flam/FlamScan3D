package com.yourorg.objectcapture.tests

import com.yourorg.objectcapture.capture.FrameMetrics
import com.yourorg.objectcapture.capture.FrameSelector
import com.yourorg.objectcapture.capture.TrackingState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameSelectorTest {
    @Test
    fun acceptsGoodFrame() {
        val selector = FrameSelector()
        val metrics = FrameMetrics(
            poseDeltaDegrees = 10.0,
            translationDeltaMeters = 0.1,
            sharpness = 120.0,
            depthMean = 0.6,
            depthStdDev = 0.02,
            exposureDelta = 0.0,
            trackingState = TrackingState.TRACKING,
            featureCount = 400
        )

        val result = selector.evaluate(metrics)
        assertTrue(result.accepted)
    }

    @Test
    fun rejectsBlurryFrame() {
        val selector = FrameSelector()
        val metrics = FrameMetrics(
            poseDeltaDegrees = 10.0,
            translationDeltaMeters = 0.1,
            sharpness = 10.0,
            depthMean = 0.6,
            depthStdDev = 0.02,
            exposureDelta = 0.0,
            trackingState = TrackingState.TRACKING,
            featureCount = 400
        )

        val result = selector.evaluate(metrics)
        assertFalse(result.accepted)
    }
}
