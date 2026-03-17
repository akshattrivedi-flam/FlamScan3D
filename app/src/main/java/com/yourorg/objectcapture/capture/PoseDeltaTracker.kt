package com.yourorg.objectcapture.capture

import com.google.ar.core.Pose
import kotlin.math.acos
import kotlin.math.min

class PoseDeltaTracker {
    private var lastAcceptedPose: Pose? = null

    fun computeDelta(current: Pose?): PoseDelta {
        val last = lastAcceptedPose
        if (current == null || last == null) {
            return PoseDelta(0.0, 0.0)
        }

        val translationDelta = distance(current.translation, last.translation)
        val angleDelta = angleBetween(current, last)

        return PoseDelta(angleDelta, translationDelta)
    }

    fun accept(pose: Pose?) {
        if (pose != null) {
            lastAcceptedPose = pose
        }
    }

    private fun distance(a: FloatArray, b: FloatArray): Double {
        val dx = (a[0] - b[0]).toDouble()
        val dy = (a[1] - b[1]).toDouble()
        val dz = (a[2] - b[2]).toDouble()
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun angleBetween(a: Pose, b: Pose): Double {
        val delta = b.inverse().compose(a)
        val q = delta.rotationQuaternion
        val w = q[3].toDouble()
        val clamped = min(1.0, kotlin.math.abs(w))
        return Math.toDegrees(2.0 * acos(clamped))
    }
}

data class PoseDelta(
    val angleDegrees: Double,
    val translationMeters: Double
)
