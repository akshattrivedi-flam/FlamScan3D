package com.yourorg.objectcapture.capture

import com.yourorg.objectcapture.ar.ARCoreManager
import com.yourorg.objectcapture.ar.PoseUtils
import com.yourorg.objectcapture.colmap.ColmapExporter
import com.yourorg.objectcapture.core.CaptureMetricsStore
import com.yourorg.objectcapture.core.FeedbackManager
import com.yourorg.objectcapture.core.OrbitManager
import com.yourorg.objectcapture.core.SessionScoreManager
import com.yourorg.objectcapture.camera.CameraController
import com.yourorg.objectcapture.camera.CameraIntrinsicsProvider
import com.yourorg.objectcapture.camera.VideoSyncWriter
import com.yourorg.objectcapture.model.Vector3
import com.yourorg.objectcapture.storage.MetadataWriter
import com.yourorg.objectcapture.storage.FrameMetadata
import com.yourorg.objectcapture.storage.PoseMetadata
import com.yourorg.objectcapture.storage.IntrinsicsMetadata
import com.yourorg.objectcapture.storage.DepthMetadata
import com.yourorg.objectcapture.storage.DraftManager
import com.yourorg.objectcapture.storage.DraftState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureController @Inject constructor(
    private val arCoreManager: ARCoreManager,
    private val cameraController: CameraController,
    private val frameProcessor: FrameProcessor,
    private val metadataWriter: MetadataWriter,
    private val colmapExporter: ColmapExporter,
    private val orbitManager: OrbitManager,
    private val feedbackManager: FeedbackManager,
    private val poseDeltaTracker: PoseDeltaTracker,
    private val exposureTracker: ExposureTracker,
    private val featureDetector: FeatureDetector,
    private val intrinsicsProvider: CameraIntrinsicsProvider,
    private val sessionScoreManager: SessionScoreManager,
    private val captureMetricsStore: CaptureMetricsStore,
    private val draftManager: DraftManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeSession: CaptureSession? = null
    private var lastFrame: CameraFrame? = null
    private var lastDepthGuidance: String? = null
    private var guidanceMinDistance = 0.4
    private var guidanceMaxDistance = 0.8
    private var markerDistanceMeters: Double? = null
    private val markerToleranceMeters = 0.15
    private var previewStarted = false
    private var syncWriter: VideoSyncWriter? = null

    fun startPreview() {
        if (previewStarted) return
        cameraController.start(null) { frame -> lastFrame = frame }
        previewStarted = true
    }

    fun stopPreview() {
        if (!previewStarted) return
        cameraController.stop()
        previewStarted = false
    }

    fun setDistanceMarker() {
        val depth = arCoreManager.latestDepthStats().meanMeters
        if (depth <= 0.0) {
            feedbackManager.postWarning("Move into view to set marker distance")
            return
        }
        markerDistanceMeters = depth
        guidanceMinDistance = (depth - markerToleranceMeters).coerceAtLeast(0.1)
        guidanceMaxDistance = depth + markerToleranceMeters
        feedbackManager.postInfo("Marker set: ${"%.2f".format(depth)} m")
    }

    fun clearDistanceMarker() {
        markerDistanceMeters = null
        guidanceMinDistance = 0.4
        guidanceMaxDistance = 0.8
        feedbackManager.postInfo("Marker cleared")
    }

    fun isMarkerSet(): Boolean = markerDistanceMeters != null

    fun start(session: CaptureSession) {
        activeSession = session
        // Start sensor-based pose tracking (no camera conflict with CameraX)
        arCoreManager.start()
        syncWriter = VideoSyncWriter(java.io.File(session.metadataDir, "video_sync.csv"))
        // CameraX already running from startPreview(); rebind with session so AE locks
        cameraController.start(sharedCameraId = null, captureSession = session) { frame ->
            lastFrame = frame
            scope.launch {
                handleFrame(frame, forceAccept = false)
            }
        }
    }

    fun stop() {
        cameraController.stop()        // unlocks AE and unbinds camera
        cameraController.stopRecording()
        arCoreManager.stop()
        activeSession = null
        syncWriter = null
        previewStarted = false         // allow preview to restart when returning to capture screen
    }

    fun captureManual() {
        val frame = lastFrame ?: return
        scope.launch {
            handleFrame(frame, forceAccept = true)
        }
    }

    fun setObjectCenterFromTap(xPx: Float, yPx: Float, width: Int, height: Int) {
        val pose = arCoreManager.hitTest(xPx, yPx, width, height) ?: return
        val t = pose.translation
        orbitManager.setObjectCenter(Vector3(t[0], t[1], t[2]))
        feedbackManager.postInfo("Anchor set")
    }

    fun saveDraft() {
        val session = activeSession ?: return
        val bins = orbitManager.getCoverageBins().toList()
        val pose = arCoreManager.latestPose()
        val draft = DraftState(
            sessionId = session.sessionId,
            sessionPath = session.rootDir.absolutePath,
            imageCount = session.imagesDir.listFiles()?.size ?: 0,
            coverageBins = bins,
            lastPoseTranslation = pose?.translation?.toList(),
            lastPoseRotation = pose?.rotationQuaternion?.toList()
        )
        draftManager.saveDraft(session.rootDir, draft)
        feedbackManager.postInfo("Draft saved")
    }

    fun applyDraft(draft: DraftState) {
        val pose = draft.toPose()
        if (pose != null) {
            poseDeltaTracker.accept(pose)
        }
    }

    private fun handleFrame(frame: CameraFrame, forceAccept: Boolean) {
        val session = activeSession ?: return
        // Pose comes from device rotation-vector sensor (no ARCore session needed)
        val pose = arCoreManager.latestPose()
        val trackingState = if (pose != null)
            com.google.ar.core.TrackingState.TRACKING
        else
            com.google.ar.core.TrackingState.PAUSED
        val depthStats = arCoreManager.latestDepthStats()

        val poseDelta = poseDeltaTracker.computeDelta(pose)
        val exposureDelta = exposureTracker.delta(frame.lumaMean)
        val featureCount = featureDetector.countFeatures(frame.grayscale, frame.width, frame.height)

        val depthMean = depthStats.meanMeters
        val depthGuidance = when {
            depthMean <= 0.0 -> null
            depthMean < guidanceMinDistance -> "Move closer"
            depthMean > guidanceMaxDistance -> "Move farther away"
            else -> null
        }
        if (depthGuidance != lastDepthGuidance) {
            depthGuidance?.let { feedbackManager.postWarning(it) }
            lastDepthGuidance = depthGuidance
        }
        captureMetricsStore.updateDepth(
            com.yourorg.objectcapture.core.DepthGuidance(
                distanceMeters = depthMean,
                inRange = depthGuidance == null && depthMean > 0.0,
                message = depthGuidance ?: "",
                minMeters = guidanceMinDistance,
                maxMeters = guidanceMaxDistance,
                markerActive = markerDistanceMeters != null
            )
        )
        if (!forceAccept && depthGuidance != null) {
            return
        }

        val sample = FrameSample(
            grayscale = frame.grayscale,
            width = frame.width,
            height = frame.height,
            poseDeltaDegrees = poseDelta.angleDegrees,
            translationDeltaMeters = poseDelta.translationMeters,
            depthMean = depthMean,
            depthStdDev = depthStats.stdDevMeters,
            exposureDelta = exposureDelta,
            trackingState = when (trackingState) {
                com.google.ar.core.TrackingState.TRACKING -> TrackingState.TRACKING
                com.google.ar.core.TrackingState.PAUSED -> TrackingState.PAUSED
                com.google.ar.core.TrackingState.STOPPED -> TrackingState.STOPPED
                else -> TrackingState.PAUSED
            },
            featureCount = featureCount
        )

        val result = frameProcessor.processFrame(sample)
        if (forceAccept || result.accepted) {
            val colmapPose = pose?.let { PoseUtils.arPoseToColmap(it) } ?: return
            val poseMatrix = pose?.let { PoseUtils.poseMatrix4x4(it) } ?: emptyList()
            val intrinsics = intrinsicsProvider.getBackCameraIntrinsics()
            val timestamp = frame.timestampMs

            cameraController.saveFrame(session, onSaved = { savedFile ->
                val metadataFile = java.io.File(session.metadataDir, savedFile.name.replace(".jpg", ".json"))

                val metadata = FrameMetadata(
                    image = "Images/${savedFile.name}",
                    timestamp_ms = timestamp,
                    pose = PoseMetadata(
                        arcore_pose_matrix = poseMatrix,
                        colmap_q = listOf(colmapPose.qw, colmapPose.qx, colmapPose.qy, colmapPose.qz),
                        colmap_t = listOf(colmapPose.tx, colmapPose.ty, colmapPose.tz)
                    ),
                    intrinsics = IntrinsicsMetadata(
                        width = intrinsics.width,
                        height = intrinsics.height,
                        fx = intrinsics.fx,
                        fy = intrinsics.fy,
                        cx = intrinsics.cx,
                        cy = intrinsics.cy,
                        distortion = intrinsics.distortion
                    ),
                    depth = DepthMetadata(
                        median_m = depthStats.medianMeters,
                        mean_m = depthStats.meanMeters,
                        stddev_m = depthStats.stdDevMeters
                    ),
                    tracking_state = sample.trackingState.name,
                    sharpness = result.sharpness,
                    feature_count = featureCount,
                    frame_score = result.score
                )
                metadataWriter.writeFrameMetadata(metadataFile, metadata)

                val videoTimeMs = cameraController.getVideoTimeMs(frame.timestampNs)
                syncWriter?.append(savedFile.name, frame.timestampNs, videoTimeMs)

                val position = pose?.translation ?: floatArrayOf(0f, 0f, 0f)
                orbitManager.updateCoverage(Vector3(position[0], position[1], position[2]))
                feedbackManager.postInfo("Captured frame ${savedFile.name}")
                val coverage = orbitManager.coverageRatio()
                val sessionScore = sessionScoreManager.update(result.score, coverage)
                captureMetricsStore.updateSessionScore(sessionScore)
                if (sessionScore.guidanceChanged) {
                    feedbackManager.postInfo(sessionScore.guidance)
                }
                val orbitSuggestion = orbitManager.recommendedBandSuggestion()
                if (orbitSuggestion.message.isNotEmpty()) {
                    feedbackManager.postInfo(orbitSuggestion.message)
                }
                captureMetricsStore.updateOrbitSuggestion(orbitSuggestion)
                poseDeltaTracker.accept(pose)
                exposureTracker.accept(frame.lumaMean)
            })
        }
    }
}

data class CameraFrame(
    val grayscale: ByteArray,
    val width: Int,
    val height: Int,
    val timestampNs: Long,
    val timestampMs: Long,
    val lumaMean: Double
)
