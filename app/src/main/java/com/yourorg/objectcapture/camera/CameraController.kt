package com.yourorg.objectcapture.camera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.google.ar.core.SharedCamera
import com.yourorg.objectcapture.ar.ARCoreManager
import com.yourorg.objectcapture.capture.CameraFrame
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val arCoreManager: ARCoreManager,
    private val videoRecorder: VideoRecorder
) {
    private var previewView: TextureView? = null
    private var previewSurface: Surface? = null

    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private var analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var cameraId: String? = null
    private var isBound: Boolean = false
    private var aeLocked: Boolean = false

    private var sharedCamera: SharedCamera? = null
    private var arCoreSurfaces: List<Surface> = emptyList()
    private var repeatingSurfaces: List<Surface> = emptyList()
    private var sessionSurfaces: List<Surface> = emptyList()

    private var analysisReader: ImageReader? = null
    private var jpegReader: ImageReader? = null

    private var frameListener: ((CameraFrame) -> Unit)? = null
    private var pendingStart: StartRequest? = null
    private val requestToken = AtomicInteger(0)
    private var activeToken: Int = 0

    private val captureCounter = AtomicInteger(0)
    private val pendingCaptures = ArrayDeque<PendingCapture>()

    private data class StartRequest(
        val sharedCameraId: String?,
        val captureSession: com.yourorg.objectcapture.capture.CaptureSession?,
        val onFrame: (CameraFrame) -> Unit,
        val onError: (Throwable) -> Unit,
        val token: Int
    )

    private data class PendingCapture(
        val file: File,
        val onSaved: (File) -> Unit,
        val onError: (Throwable) -> Unit
    )

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {}

    fun attachPreview(previewView: TextureView) {
        this.previewView = previewView
        previewView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                pendingStart?.let { request ->
                    pendingStart = null
                    startInternal(request)
                }
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                // No-op. We keep the configured buffer size for the session.
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                stop()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
        if (previewView.isAvailable) {
            pendingStart?.let { request ->
                pendingStart = null
                startInternal(request)
            }
        }
    }

    fun start(
        sharedCameraId: String? = null,
        captureSession: com.yourorg.objectcapture.capture.CaptureSession? = null,
        onFrame: (CameraFrame) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        frameListener = onFrame
        val token = requestToken.incrementAndGet()
        val request = StartRequest(sharedCameraId, captureSession, onFrame, onError, token)
        val preview = previewView
        if (preview == null || !preview.isAvailable) {
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
        try {
            activeToken = request.token
            ensureCameraThread()
            val handler = cameraHandler ?: return
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val selectedCameraId = request.sharedCameraId ?: selectBackCamera(manager)
            cameraId = selectedCameraId

            val characteristics = manager.getCameraCharacteristics(selectedCameraId)
            val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val previewSize = chooseSize(configMap?.getOutputSizes(SurfaceTexture::class.java), 1920, 1080)
            val analysisSize = chooseSize(configMap?.getOutputSizes(ImageFormat.YUV_420_888), 1280, 720)
            val jpegSize = chooseMaxSize(configMap?.getOutputSizes(ImageFormat.JPEG)) ?: previewSize

            val texture = previewView?.surfaceTexture ?: return
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            previewSurface = Surface(texture)

            arCoreManager.setSharedCameraBufferSize(previewSize.width, previewSize.height)

            analysisReader?.close()
            analysisReader = ImageReader.newInstance(
                analysisSize.width,
                analysisSize.height,
                ImageFormat.YUV_420_888,
                2
            ).also { reader ->
                reader.setOnImageAvailableListener({ r ->
                    val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val frame = buildFrame(image)
                    image.close()
                    if (frame != null) {
                        analysisExecutor.execute { frameListener?.invoke(frame) }
                    }
                }, handler)
            }

            jpegReader?.close()
            jpegReader = ImageReader.newInstance(
                jpegSize.width,
                jpegSize.height,
                ImageFormat.JPEG,
                2
            ).also { reader ->
                reader.setOnImageAvailableListener({ r ->
                    val image = r.acquireNextImage() ?: return@setOnImageAvailableListener
                    handleJpegImage(image)
                }, handler)
            }

            val preview = previewSurface
            val analysisSurface = analysisReader?.surface
            val jpegSurface = jpegReader?.surface
            if (preview == null || analysisSurface == null || jpegSurface == null) {
                request.onError(IllegalStateException("Missing camera surfaces"))
                return
            }

            val appSurfaces = listOf(preview, analysisSurface, jpegSurface)
            sharedCamera = arCoreManager.getSharedCamera()
            sharedCamera?.setAppSurfaces(selectedCameraId, appSurfaces)

            val deviceCallback = object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    if (!isRequestActive(request.token)) {
                        device.close()
                        return
                    }
                    cameraDevice = device
                    arCoreSurfaces = try {
                        sharedCamera?.arCoreSurfaces ?: emptyList()
                    } catch (t: Throwable) {
                        device.close()
                        request.onError(t)
                        return
                    }
                    repeatingSurfaces = buildList {
                        addAll(arCoreSurfaces)
                        add(preview)
                        add(analysisSurface)
                    }
                    sessionSurfaces = buildList {
                        addAll(arCoreSurfaces)
                        addAll(appSurfaces)
                    }
                    createCaptureSession(device, handler, request)
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                    if (isRequestActive(request.token)) {
                        request.onError(CameraAccessException(CameraAccessException.CAMERA_DISCONNECTED))
                    }
                }

                override fun onError(device: CameraDevice, error: Int) {
                    device.close()
                    if (isRequestActive(request.token)) {
                        request.onError(CameraAccessException(CameraAccessException.CAMERA_ERROR))
                    }
                }
            }

            val openCallback = sharedCamera?.createARDeviceStateCallback(deviceCallback, handler) ?: deviceCallback
            manager.openCamera(selectedCameraId, openCallback, handler)
        } catch (t: Throwable) {
            request.onError(t)
        }
    }

    private fun createCaptureSession(
        device: CameraDevice,
        handler: Handler,
        request: StartRequest
    ) {
        val sessionCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                if (!isRequestActive(request.token) || cameraDevice != device) {
                    session.close()
                    return
                }
                captureSession = session
                sharedCamera?.setCaptureCallback(captureCallback, handler)
                startRepeating(session, handler)
                arCoreManager.resumeSessionIfNeeded()
                isBound = true
                request.captureSession?.let { setAutoExposureLocked(true) }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                request.onError(IllegalStateException("Camera session configuration failed"))
            }
        }

        val callback = sharedCamera?.createARSessionStateCallback(sessionCallback, handler) ?: sessionCallback
        device.createCaptureSession(sessionSurfaces, callback, handler)
    }

    private fun startRepeating(session: CameraCaptureSession, handler: Handler) {
        if (!isRequestActive(activeToken)) return
        val device = cameraDevice ?: return
        try {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            for (surface in repeatingSurfaces) {
                builder.addTarget(surface)
            }
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            builder.set(CaptureRequest.CONTROL_AE_LOCK, aeLocked)
            session.setRepeatingRequest(builder.build(), captureCallback, handler)
        } catch (_: Throwable) {
            // Ignore repeating failures when the camera is closing.
        }
    }

    fun stop() {
        requestToken.incrementAndGet()
        try {
            val session = captureSession
            val device = cameraDevice
            captureSession = null
            cameraDevice = null
            if (session != null || device != null) {
                cameraHandler?.post {
                    try {
                        session?.close()
                    } catch (_: Throwable) {}
                    try {
                        device?.close()
                    } catch (_: Throwable) {}
                } ?: run {
                    session?.close()
                    device?.close()
                }
            }
        } catch (_: Throwable) {
        }

        analysisReader?.close()
        analysisReader = null
        jpegReader?.close()
        jpegReader = null

        previewSurface?.release()
        previewSurface = null

        analysisExecutor.shutdown()
        analysisExecutor = Executors.newSingleThreadExecutor()

        pendingStart = null
        frameListener = null
        pendingCaptures.clear()
        isBound = false
        arCoreSurfaces = emptyList()
        repeatingSurfaces = emptyList()
        sessionSurfaces = emptyList()
        sharedCamera = null
        cameraId = null
    }

    private fun isRequestActive(token: Int): Boolean {
        return token == requestToken.get()
    }

    fun saveFrame(
        session: com.yourorg.objectcapture.capture.CaptureSession,
        onSaved: (File) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        val jpegSurface = jpegReader?.surface
        val device = cameraDevice
        val sessionCapture = captureSession
        val handler = cameraHandler
        if (jpegSurface == null || device == null || sessionCapture == null || handler == null) {
            onError(IllegalStateException("Camera not ready"))
            return
        }

        val index = captureCounter.incrementAndGet()
        val file = File(session.imagesDir, String.format("frame_%06d.jpg", index))
        val pending = PendingCapture(file, onSaved, onError)
        synchronized(pendingCaptures) {
            pendingCaptures.addLast(pending)
            if (pendingCaptures.size > 1) {
                return
            }
        }

        issueStillCapture(device, sessionCapture, handler)
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
        aeLocked = locked
        val session = captureSession ?: return
        val handler = cameraHandler ?: return
        startRepeating(session, handler)
    }

    private fun issueStillCapture(
        device: CameraDevice,
        session: CameraCaptureSession,
        handler: Handler
    ) {
        try {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            jpegReader?.surface?.let { builder.addTarget(it) }
            for (surface in arCoreSurfaces) {
                builder.addTarget(surface)
            }
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            builder.set(CaptureRequest.CONTROL_AE_LOCK, aeLocked)
            session.capture(builder.build(), captureCallback, handler)
        } catch (t: Throwable) {
            synchronized(pendingCaptures) {
                val pending = pendingCaptures.pollFirst() ?: return
                pending.onError(t)
            }
            val next = synchronized(pendingCaptures) { pendingCaptures.peekFirst() }
            if (next != null) {
                val nextDevice = cameraDevice
                val nextSession = captureSession
                val nextHandler = cameraHandler
                if (nextDevice != null && nextSession != null && nextHandler != null) {
                    issueStillCapture(nextDevice, nextSession, nextHandler)
                }
            }
        }
    }

    private fun handleJpegImage(image: Image) {
        val pending = synchronized(pendingCaptures) {
            pendingCaptures.pollFirst()
        } ?: run {
            image.close()
            return
        }

        try {
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            image.close()

            FileOutputStream(pending.file).use { out ->
                out.write(data)
            }
            pending.onSaved(pending.file)
        } catch (t: Throwable) {
            try {
                image.close()
            } catch (_: Throwable) {}
            pending.onError(t)
        } finally {
            val next = synchronized(pendingCaptures) { pendingCaptures.peekFirst() }
            if (next != null) {
                val device = cameraDevice
                val session = captureSession
                val handler = cameraHandler
                if (device != null && session != null && handler != null) {
                    issueStillCapture(device, session, handler)
                }
            }
        }
    }

    private fun buildFrame(image: Image): CameraFrame? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val meanLuma = if (bytes.isNotEmpty()) {
            var sum = 0L
            for (b in bytes) {
                sum += (b.toInt() and 0xFF)
            }
            sum.toDouble() / bytes.size.toDouble()
        } else {
            0.0
        }

        val timestampNs = image.timestamp
        val timestampMs = timestampNs / 1_000_000

        return CameraFrame(
            grayscale = bytes,
            width = image.width,
            height = image.height,
            timestampNs = timestampNs,
            timestampMs = timestampMs,
            lumaMean = meanLuma
        )
    }

    private fun ensureCameraThread() {
        if (cameraThread != null) return
        cameraThread = HandlerThread("Camera2Thread").also { it.start() }
        cameraHandler = Handler(cameraThread!!.looper)
    }

    private fun selectBackCamera(manager: CameraManager): String {
        val ids = manager.cameraIdList
        for (id in ids) {
            val chars = manager.getCameraCharacteristics(id)
            if (chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                return id
            }
        }
        return ids.first()
    }

    private fun chooseSize(sizes: Array<Size>?, targetWidth: Int, targetHeight: Int): Size {
        if (sizes == null || sizes.isEmpty()) return Size(targetWidth, targetHeight)
        val targetRatio = targetWidth.toDouble() / targetHeight.toDouble()
        val sorted = sizes.sortedBy { it.width * it.height }
        val ratioMatches = sorted.filter {
            val ratio = it.width.toDouble() / it.height.toDouble()
            kotlin.math.abs(ratio - targetRatio) < 0.02
        }
        val candidates = if (ratioMatches.isNotEmpty()) ratioMatches else sorted
        return candidates.firstOrNull { it.width >= targetWidth && it.height >= targetHeight }
            ?: candidates.last()
    }

    private fun chooseMaxSize(sizes: Array<Size>?): Size? {
        if (sizes == null || sizes.isEmpty()) return null
        return sizes.maxByOrNull { it.width * it.height }
    }
}
