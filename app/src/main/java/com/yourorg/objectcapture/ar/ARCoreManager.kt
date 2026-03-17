package com.yourorg.objectcapture.ar

import android.content.Context
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ARCoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val depthDistanceController: DepthDistanceController
) {
    private var session: Session? = null
    private var latestFrame: Frame? = null
    private var useSharedCamera: Boolean = true

    fun enableSharedCamera(enable: Boolean) {
        useSharedCamera = enable
    }

    fun start() {
        if (session == null) {
            session = try {
                if (useSharedCamera) {
                    Session(context, setOf(Session.Feature.SHARED_CAMERA))
                } else {
                    Session(context)
                }
            } catch (t: Throwable) {
                Session(context)
            }
            val config = Config(session).apply {
                depthMode = Config.DepthMode.AUTOMATIC
            }
            session?.configure(config)
        }
        session?.resume()
    }

    fun stop() {
        session?.pause()
    }

    fun update(): Frame? {
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
        return latestFrame?.camera?.pose
    }

    fun latestDepthStats(): DepthStats {
        val frame = latestFrame ?: return DepthStats()
        return depthDistanceController.computeDepthStats(frame)
    }

    fun hitTest(xPx: Float, yPx: Float, viewWidth: Int, viewHeight: Int): Pose? {
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
        return session?.cameraConfig?.cameraId
    }
}

data class DepthStats(
    val medianMeters: Double = 0.0,
    val meanMeters: Double = 0.0,
    val stdDevMeters: Double = 0.0
)
