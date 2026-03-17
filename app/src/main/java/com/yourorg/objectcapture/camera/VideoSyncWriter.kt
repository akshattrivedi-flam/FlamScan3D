package com.yourorg.objectcapture.camera

import java.io.File

class VideoSyncWriter(private val outputFile: File) {
    init {
        if (!outputFile.exists()) {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText("image_name,image_timestamp_ns,video_time_ms\n")
        }
    }

    fun append(imageName: String, imageTimestampNs: Long, videoTimeMs: Long) {
        outputFile.appendText("$imageName,$imageTimestampNs,$videoTimeMs\n")
    }
}
