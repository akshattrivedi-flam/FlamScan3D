package com.yourorg.objectcapture.camera

import android.content.Context
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
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

    fun attachPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        this.previewView = previewView
        this.lifecycleOwner = lifecycleOwner
    }

    fun start(sharedCameraId: String? = null, onFrame: (CameraFrame) -> Unit) {
        val previewView = previewView ?: return
        val lifecycleOwner = lifecycleOwner ?: return

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageCapture = ImageCapture.Builder().build()
            imageCaptureController.bind(imageCapture)

            val analysis = ImageAnalysis.Builder().build().also { analyzer ->
                analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
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
                    onFrame(frame)
                    imageProxy.close()
                }
            }

            val cameraSelector = if (sharedCameraId != null) {
                CameraSelector.Builder()
                    .addCameraFilter { cameras ->
                        cameras.filter { Camera2CameraInfo.from(it).cameraId == sharedCameraId }
                    }
                    .build()
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            val videoCapture = videoRecorder.createUseCase()

            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture, analysis, videoCapture)
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        cameraExecutor.shutdown()
        cameraExecutor = Executors.newSingleThreadExecutor()
        camera = null
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
