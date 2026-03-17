package com.yourorg.objectcapture.network

import com.yourorg.objectcapture.capture.CaptureSession
import com.yourorg.objectcapture.storage.ZipUtils
import java.io.File

class UploadManager(
    private val apiService: ApiService
) {
    fun uploadSession(session: CaptureSession, onProgress: (Int) -> Unit): String {
        val zipFile = File(session.rootDir, "capture_upload.zip")
        ZipUtils.zipDirectory(session.rootDir, zipFile)
        return apiService.uploadZip(zipFile, onProgress)
    }
}
