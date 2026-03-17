package com.yourorg.objectcapture.capture

import kotlin.math.abs

class FeatureDetector(
    private val gradientThreshold: Int = 20
) {
    fun countFeatures(luma: ByteArray, width: Int, height: Int): Int {
        if (luma.isEmpty() || width < 3 || height < 3) return 0
        var count = 0
        fun get(x: Int, y: Int): Int = luma[y * width + x].toInt() and 0xFF
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = get(x + 1, y) - get(x - 1, y)
                val gy = get(x, y + 1) - get(x, y - 1)
                val mag = abs(gx) + abs(gy)
                if (mag > gradientThreshold) count++
            }
        }
        return count
    }
}
