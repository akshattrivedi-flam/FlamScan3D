package com.yourorg.objectcapture.ar

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin

@Singleton
class ARCoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val depthDistanceController: DepthDistanceController
) {
    private var session: Session? = null
    private var latestFrame: Frame? = null
    private var useSharedCamera: Boolean = true
    private var useSensorFallback: Boolean = false

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

    fun enableSharedCamera(enable: Boolean) {
        useSharedCamera = enable
    }

    fun isUsingSensorFallback(): Boolean = useSensorFallback

    fun start() {
        if (useSensorFallback) {
            startSensors()
            return
        }

        if (session == null) {
            session = try {
                if (useSharedCamera) {
                    Session(context, setOf(Session.Feature.SHARED_CAMERA))
                } else {
                    Session(context)
                }
            } catch (t: Throwable) {
                useSensorFallback = true
                startSensors()
                return
            }
            val config = Config(session).apply {
                depthMode = Config.DepthMode.AUTOMATIC
            }
            session?.configure(config)
        }

        try {
            session?.resume()
        } catch (t: Throwable) {
            useSensorFallback = true
            startSensors()
            return
        }

        if (useSharedCamera && session?.cameraConfig?.cameraId == null) {
            session?.pause()
            session = null
            useSensorFallback = true
            startSensors()
        }
    }

    fun stop() {
        session?.pause()
        stopSensors()
    }

    fun update(): Frame? {
        if (useSensorFallback) return null
        val session = session ?: return null
        return try {
            val frame = session.update()
            latestFrame = frame
            frame
        } catch (t: Throwable) {
            null
        }
    }

    fun latestPose(): Pose? {
        return if (useSensorFallback) {
            sensorPose()
        } else {
            latestFrame?.camera?.pose
        }
    }

    fun latestDepthStats(): DepthStats {
        if (useSensorFallback) return DepthStats()
        val frame = latestFrame ?: return DepthStats()
        return depthDistanceController.computeDepthStats(frame)
    }

    fun hitTest(xPx: Float, yPx: Float, viewWidth: Int, viewHeight: Int): Pose? {
        if (useSensorFallback) return null
        val frame = latestFrame ?: return null
        val hits = frame.hitTest(xPx, yPx)
        for (hit in hits) {
            val trackable = hit.trackable
            if (trackable.trackingState == TrackingState.TRACKING) {
                return hit.hitPose
            }
        }
        return null
    }

    fun getCameraId(): String? {
        return if (useSensorFallback) null else session?.cameraConfig?.cameraId
    }

    private fun startSensors() {
        if (sensorActive) return
        rotationSensor?.let {
            sensorManager.registerListener(
                sensorListener, it, SensorManager.SENSOR_DELAY_GAME
            )
            sensorActive = true
        }
    }

    private fun stopSensors() {
        if (!sensorActive) return
        sensorManager.unregisterListener(sensorListener)
        sensorActive = false
    }

    private fun sensorPose(): Pose? {
        val rv = rotationVector
        if (rv[0] == 0f && rv[1] == 0f && rv[2] == 0f) return null

        val R = FloatArray(16)
        SensorManager.getRotationMatrixFromVector(R, rv)

        val orientation = FloatArray(3)
        SensorManager.getOrientation(R, orientation)
        val azimuth = orientation[0].toDouble()
        val pitch = orientation[1].toDouble()

        val tx = (cos(pitch) * sin(azimuth)).toFloat()
        val ty = sin(pitch).toFloat()
        val tz = (cos(pitch) * cos(azimuth)).toFloat()

        val q = FloatArray(4)
        SensorManager.getQuaternionFromVector(q, rv)

        return Pose(floatArrayOf(tx, ty, tz), floatArrayOf(q[1], q[2], q[3], q[0]))
    }
}

data class DepthStats(
    val medianMeters: Double = 0.0,
    val meanMeters: Double = 0.0,
    val stdDevMeters: Double = 0.0
)
