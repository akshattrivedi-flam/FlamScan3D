package com.yourorg.objectcapture.capture

class ExposureTracker {
    private var lastAcceptedLuma: Double? = null

    fun delta(currentLuma: Double): Double {
        val last = lastAcceptedLuma ?: return 0.0
        return currentLuma - last
    }

    fun accept(currentLuma: Double) {
        lastAcceptedLuma = currentLuma
    }
}
