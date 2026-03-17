package com.yourorg.objectcapture.camera

import android.content.Context
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.yourorg.objectcapture.capture.CameraFrame
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCamera2Interop::class)
@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCaptureController: ImageCaptureController,
    private val videoRecorder: VideoRecorder
) {
    private var previewView: PreviewView? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var frameListener: ((CameraFrame) -> Unit)? = null
    private var pendingStart: StartRequest? = null
    private var isBound: Boolean = false

    private data class StartRequest(
        val sharedCameraId: String?,
        val captureSession: com.yourorg.objectcapture.capture.CaptureSession?,
        val onFrame: (CameraFrame) -> Unit,
        val onError: (Throwable) -> Unit
    )

    fun attachPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        this.previewView = previewView
        this.lifecycleOwner = lifecycleOwner
        pendingStart?.let { request ->
            pendingStart = null
            startInternal(request)
        }
    }

    fun start(
        sharedCameraId: String? = null,
        captureSession: com.yourorg.objectcapture.capture.CaptureSession? = null,
        onFrame: (CameraFrame) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        frameListener = onFrame
        val request = StartRequest(sharedCameraId, captureSession, onFrame, onError)
        if (previewView == null || lifecycleOwner == null) {
            pendingStart = request
            return
        }
        if (isBound) {
            captureSession?.let { setAutoExposureLocked(true) }
            return
        }
        startInternal(request)
    }

    private fun startInternal(request: StartRequest) {
        val previewView = previewView ?: return
        val lifecycleOwner = lifecycleOwner ?: return

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setJpegQuality(90)
                    .build()
                imageCaptureController.bind(imageCapture)

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also { analyzer ->
                        analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                            try {
                                val yPlane = imageProxy.planes.firstOrNull()
                                val buffer = yPlane?.buffer
                                val byteArray = if (buffer != null) {
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    bytes
                                } else {
                                    ByteArray(0)
                                }
                                val meanLuma = if (byteArray.isNotEmpty()) {
                                    var sum = 0L
                                    for (b in byteArray) {
                                        sum += (b.toInt() and 0xFF)
                                    }
                                    sum.toDouble() / byteArray.size.toDouble()
                                } else {
                                    0.0
                                }
                                val timestampNs = imageProxy.imageInfo.timestamp
                                val timestampMs = timestampNs / 1_000_000

                                val frame = CameraFrame(
                                    grayscale = byteArray,
                                    width = imageProxy.width,
                                    height = imageProxy.height,
                                    timestampNs = timestampNs,
                                    timestampMs = timestampMs,
                                    lumaMean = meanLuma
                                )
                                frameListener?.invoke(frame)
                            } finally {
                                imageProxy.close()
                            }
                        }
                    }

                val cameraSelector = if (request.sharedCameraId != null) {
                    CameraSelector.Builder()
                        .addCameraFilter { cameras ->
                            cameras.filter { Camera2CameraInfo.from(it).cameraId == request.sharedCameraId }
                        }
                        .build()
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                // Bind exactly 3 use cases: Preview + ImageCapture + ImageAnalysis.
                // Adding VideoCapture as a 4th use case exceeds the concurrent-use
                // limit on most Android devices and causes bindToLifecycle to fail,
                // resulting in a permanently black camera preview.
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture, analysis
                )
                isBound = true

                request.captureSession?.let {
                    setAutoExposureLocked(true)
                }
            } catch (t: Throwable) {
                isBound = false
                request.onError(t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        setAutoExposureLocked(false)
        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraExecutor.shutdown()
        cameraExecutor = Executors.newSingleThreadExecutor()
        camera = null
        isBound = false
        pendingStart = null
        frameListener = null
    }

    fun saveFrame(
        session: com.yourorg.objectcapture.capture.CaptureSession,
        onSaved: (java.io.File) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        imageCaptureController.saveImage(session, onSaved, onError)
    }

    fun startRecording(session: com.yourorg.objectcapture.capture.CaptureSession) {
        videoRecorder.startRecording(session)
    }

    fun stopRecording() {
        videoRecorder.stopRecording()
    }

    fun getVideoTimeMs(imageTimestampNs: Long): Long {
        return videoRecorder.videoTimeMsFromTimestampNs(imageTimestampNs)
    }

    fun setAutoExposureLocked(locked: Boolean) {
        val cam = camera ?: return
        val camera2 = Camera2CameraControl.from(cam.cameraControl)
        camera2.setCaptureRequestOptions(
            androidx.camera.camera2.interop.CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, locked)
                .build()
        )
    }
}
