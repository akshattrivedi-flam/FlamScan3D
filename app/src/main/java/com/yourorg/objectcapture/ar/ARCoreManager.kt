package com.yourorg.objectcapture.ar

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.cos
import kotlin.math.sin
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides camera pose data for orbit coverage tracking and COLMAP export.
 *
 * Uses Android's ROTATION_VECTOR sensor instead of an ARCore camera session
 * to eliminate the Camera2 conflict that crashes the app when CameraX already
 * holds the back camera device.
 *
 * Device orientation is sufficient for:
 *  - Orbit-bin tracking (azimuth / elevation around the object)
 *  - Frame-to-frame pose delta (rotation angle between captures)
 *  - Approximate COLMAP pose export
 */
@Singleton
class ARCoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Suppress("UNUSED_PARAMETER")
    private val depthDistanceController: DepthDistanceController   // kept for DI
) {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    @Volatile
    private var rotationVector = FloatArray(5) { 0f }
    private var sensorActive = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            rotationVector = event.values.copyOf()
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    /** No-op: CameraX manages the back camera; no ARCore session is opened. */
    fun enableSharedCamera(enable: Boolean) = Unit

    fun start() {
        if (sensorActive) return
        rotationSensor?.let {
            sensorManager.registerListener(
                sensorListener, it, SensorManager.SENSOR_DELAY_GAME
            )
            sensorActive = true
        }
    }

    fun stop() {
        sensorManager.unregisterListener(sensorListener)
        sensorActive = false
    }

    /**
     * No ARCore frame in sensor-only mode.
     * CaptureController reads the pose directly via [latestPose].
     */
    fun update(): Frame? = null

    /**
     * Builds a synthetic [Pose] from the device rotation-vector sensor.
     *
     * • Translation = device forward unit-vector in world space so that
     *   OrbitManager sees the camera sweeping across a unit sphere as the
     *   user walks around the object.
     * • Rotation quaternion = true device orientation for COLMAP export.
     *
     * Returns null only before the first sensor event arrives.
     */
    fun latestPose(): Pose? {
        val rv = rotationVector
        if (rv[0] == 0f && rv[1] == 0f && rv[2] == 0f) return null

        // 4×4 rotation matrix from the rotation-vector sensor
        val R = FloatArray(16)
        SensorManager.getRotationMatrixFromVector(R, rv)

        // orientation[0]=azimuth (compass), [1]=pitch (forward/back tilt)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(R, orientation)
        val azimuth = orientation[0].toDouble()
        val pitch   = orientation[1].toDouble()

        // Camera position on unit sphere — drives orbit coverage bins
        val tx = (cos(pitch) * sin(azimuth)).toFloat()
        val ty = sin(pitch).toFloat()
        val tz = (cos(pitch) * cos(azimuth)).toFloat()

        // SensorManager gives [w, x, y, z]; ARCore Pose wants [qx, qy, qz, qw]
        val q = FloatArray(4)
        SensorManager.getQuaternionFromVector(q, rv)

        return Pose(floatArrayOf(tx, ty, tz), floatArrayOf(q[1], q[2], q[3], q[0]))
    }

    /**
     * No depth hardware in sensor mode.
     * Returns zero stats → depth overlay stays hidden, FrameSelector skips
     * the depth range check (depthMean == 0.0 treated as "unavailable").
     */
    fun latestDepthStats(): DepthStats = DepthStats()

    /**
     * AR hit-testing requires an ARCore session — unavailable here.
     * OrbitManager defaults the object center to the world origin (0,0,0).
     */
    fun hitTest(xPx: Float, yPx: Float, viewWidth: Int, viewHeight: Int): Pose? = null

    /** CameraX uses DEFAULT_BACK_CAMERA; no shared camera ID is needed. */
    fun getCameraId(): String? = null
}

data class DepthStats(
    val medianMeters: Double = 0.0,
    val meanMeters: Double = 0.0,
    val stdDevMeters: Double = 0.0
)
