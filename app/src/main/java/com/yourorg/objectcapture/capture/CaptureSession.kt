package com.yourorg.objectcapture.capture

import java.io.File

data class CaptureSession(
    val sessionId: String,
    val rootDir: File,
    val imagesDir: File,
    val metadataDir: File,
    val videoDir: File,
    val modelsDir: File
)
