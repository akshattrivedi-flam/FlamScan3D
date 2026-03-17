package com.yourorg.objectcapture.camera

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.yourorg.objectcapture.capture.CaptureSession
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class ImageCaptureController @Inject constructor(
    @ApplicationContext private val contextProvider: android.content.Context
) {
    private var imageCapture: ImageCapture? = null
    private val counter = AtomicInteger(0)

    fun bind(imageCapture: ImageCapture) {
        this.imageCapture = imageCapture
    }

    fun saveImage(
        session: CaptureSession,
        onSaved: (File) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val capture = imageCapture ?: return
        val index = counter.incrementAndGet()
        val file = File(session.imagesDir, String.format("frame_%06d.jpg", index))
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(contextProvider),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onSaved(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }
}
