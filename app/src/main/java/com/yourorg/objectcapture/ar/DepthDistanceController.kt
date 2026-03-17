package com.yourorg.objectcapture.ar

import com.google.ar.core.Frame
import java.nio.ShortBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class DepthDistanceController {
    fun computeDepthStats(frame: Frame): DepthStats {
        return try {
            val depthImage = frame.acquireDepthImage()
            depthImage.use { image ->
                val width = image.width
                val height = image.height
                val plane = image.planes[0]
                val buffer = plane.buffer.asShortBuffer()
                val rowStride = plane.rowStride / 2

                val patch = 100
                val startX = max(0, (width - patch) / 2)
                val startY = max(0, (height - patch) / 2)
                val endX = min(width, startX + patch)
                val endY = min(height, startY + patch)

                val values = ArrayList<Double>((endX - startX) * (endY - startY))
                for (y in startY until endY) {
                    val row = y * rowStride
                    for (x in startX until endX) {
                        val depthMm = buffer.get(row + x).toInt() and 0xFFFF
                        if (depthMm > 0) {
                            values.add(depthMm / 1000.0)
                        }
                    }
                }

                if (values.isEmpty()) return DepthStats()
                values.sort()
                val mean = values.sum() / values.size
                val median = values[values.size / 2]
                var variance = 0.0
                for (v in values) {
                    variance += (v - mean) * (v - mean)
                }
                variance /= values.size

                DepthStats(
                    medianMeters = median,
                    meanMeters = mean,
                    stdDevMeters = sqrt(variance)
                )
            }
        } catch (t: Throwable) {
            DepthStats()
        }
    }
}

private inline fun <T> T.use(block: (T) -> DepthStats): DepthStats where T : AutoCloseable {
    return try {
        block(this)
    } finally {
        close()
    }
}
