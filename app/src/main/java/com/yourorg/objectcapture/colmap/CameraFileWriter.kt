package com.yourorg.objectcapture.colmap

import java.io.File

class CameraFileWriter {
    fun write(file: File, cameraId: Int, model: String, intrinsics: CameraIntrinsics) {
        val line = buildString {
            append("$cameraId $model ${intrinsics.width} ${intrinsics.height} ")
            append("${intrinsics.fx} ${intrinsics.fy} ${intrinsics.cx} ${intrinsics.cy}")
            if (model == "OPENCV" && !intrinsics.distortion.isNullOrEmpty()) {
                append(" ")
                append(intrinsics.distortion.joinToString(" "))
            }
        }
        file.writeText(line + "\n")
    }
}
