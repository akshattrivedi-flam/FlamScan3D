package com.yourorg.objectcapture.storage

import android.content.Context
import android.os.Environment
import com.yourorg.objectcapture.capture.CaptureSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureFolderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createNewSession(): CaptureSession {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val root = File(getCapturesRoot(), timestamp)
        return ensureSessionDirs(timestamp, root)
    }

    fun openSession(root: File): CaptureSession {
        val sessionId = root.name
        return ensureSessionDirs(sessionId, root)
    }

    fun getCapturesRoot(): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File(base, "captures")
    }

    private fun ensureSessionDirs(sessionId: String, root: File): CaptureSession {
        val images = File(root, "Images")
        val metadata = File(root, "Metadata")
        val video = File(root, "Video")
        val models = File(root, "Models")

        listOf(root, images, metadata, video, models).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }

        return CaptureSession(
            sessionId = sessionId,
            rootDir = root,
            imagesDir = images,
            metadataDir = metadata,
            videoDir = video,
            modelsDir = models
        )
    }
}
