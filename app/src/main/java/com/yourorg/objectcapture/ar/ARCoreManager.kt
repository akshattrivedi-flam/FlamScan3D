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
import com.google.ar.core.SharedCamera
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
    private var sharedCamera: SharedCamera? = null
    private val glThread = ArCoreGlThread()
    private var useSharedCamera: Boolean = true
    private var useSensorFallback: Boolean = false
    private var pendingResume: Boolean = false
    private var sessionUsesSharedCamera: Boolean? = null

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    @Volatile
    private var rotationVector = FloatArray(5) { 0f }
    private var sensorActive = false

    @Volatile
    private var latestPose: Pose? = null
    @Volatile
    private var latestTrackingState: TrackingState = TrackingState.PAUSED
    @Volatile
    private var latestDepthStats: DepthStats = DepthStats()

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

        if (session != null && sessionUsesSharedCamera != useSharedCamera) {
            session?.close()
            session = null
            sharedCamera = null
            latestFrame = null
            pendingResume = false
            sessionUsesSharedCamera = null
            glThread.reset()
        }

        if (session == null) {
            session = try {
                if (useSharedCamera) {
                    Session.createForSharedCamera(context)
                } else {
                    Session(context)
                }
            } catch (t: Throwable) {
                useSensorFallback = true
                startSensors()
                return
            }
            sharedCamera = if (useSharedCamera) session?.sharedCamera else null
            glThread.reset()
            sessionUsesSharedCamera = useSharedCamera
            val config = Config(session).apply {
                depthMode = Config.DepthMode.AUTOMATIC
            }
            session?.configure(config)
            try {
                session?.let { glThread.runWithGlContext(it, sharedCamera) { Unit } }
            } catch (_: Throwable) {
                // Will retry on the first update.
            }
        }

        if (useSharedCamera) {
            pendingResume = true
        } else {
            resumeSessionOrFallback()
        }

    }

    fun stop() {
        session?.pause()
        stopSensors()
        pendingResume = false
    }

    fun updateState(): ArFrameState? {
        if (useSensorFallback) return null
        if (pendingResume) return null
        val session = session ?: return null
        return try {
            glThread.runWithGlContext(session, sharedCamera) {
                val frame = session.update()
                latestFrame = frame
                val pose = frame.camera.pose
                val tracking = frame.camera.trackingState
                val depthStats = depthDistanceController.computeDepthStats(frame)
                latestPose = pose
                latestTrackingState = tracking
                latestDepthStats = depthStats
                ArFrameState(pose, tracking, depthStats)
            }
        } catch (t: Throwable) {
            null
        }
    }

    fun resumeSessionIfNeeded() {
        if (!pendingResume) return
        val session = session ?: return
        try {
            glThread.runWithGlContext(session, sharedCamera) {
                session.resume()
            }
            pendingResume = false
        } catch (_: Throwable) {
            // Keep pending to retry once the camera is fully ready.
        }
    }

    fun latestPose(): Pose? {
        return if (useSensorFallback) {
            sensorPose()
        } else {
            latestPose
        }
    }

    fun latestTrackingState(): TrackingState {
        return if (useSensorFallback) TrackingState.PAUSED else latestTrackingState
    }

    fun latestDepthStats(): DepthStats {
        return if (useSensorFallback) DepthStats() else latestDepthStats
    }

    fun hitTest(xPx: Float, yPx: Float, viewWidth: Int, viewHeight: Int): Pose? {
        if (useSensorFallback) return null
        val session = session ?: return null
        return try {
            glThread.runWithGlContext(session, sharedCamera) {
                val frame = latestFrame ?: session.update().also { latestFrame = it }
                val hits = frame.hitTest(xPx, yPx)
                for (hit in hits) {
                    val trackable = hit.trackable
                    if (trackable.trackingState == TrackingState.TRACKING) {
                        return@runWithGlContext hit.hitPose
                    }
                }
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun getCameraId(): String? {
        return if (useSensorFallback) null else session?.cameraConfig?.cameraId
    }

    fun getSharedCamera(): SharedCamera? = if (useSensorFallback) null else sharedCamera

    fun setSharedCameraBufferSize(width: Int, height: Int) {
        glThread.setSharedCameraBufferSize(width, height, sharedCamera)
    }

    fun updateForRendering(near: Float, far: Float): ArRenderState? {
        if (useSensorFallback) return null
        if (pendingResume) return null
        val session = session ?: return null
        return try {
            glThread.runWithGlContext(session, sharedCamera) {
                val frame = session.update()
                latestFrame = frame
                val view = FloatArray(16)
                val proj = FloatArray(16)
                frame.camera.getViewMatrix(view, 0)
                frame.camera.getProjectionMatrix(proj, 0, near, far)
                val pose = frame.camera.pose
                val tracking = frame.camera.trackingState
                latestPose = pose
                latestTrackingState = tracking
                ArRenderState(view, proj, pose, tracking)
            }
        } catch (_: Throwable) {
            null
        }
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

    private fun resumeSessionOrFallback() {
        val session = session ?: return
        try {
            glThread.runWithGlContext(session, sharedCamera) {
                session.resume()
            }
        } catch (t: Throwable) {
            useSensorFallback = true
            startSensors()
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

data class ArFrameState(
    val pose: Pose?,
    val trackingState: TrackingState,
    val depthStats: DepthStats
)

data class ArRenderState(
    val viewMatrix: FloatArray,
    val projectionMatrix: FloatArray,
    val pose: Pose?,
    val trackingState: TrackingState
)

data class DepthStats(
    val medianMeters: Double = 0.0,
    val meanMeters: Double = 0.0,
    val stdDevMeters: Double = 0.0
)
