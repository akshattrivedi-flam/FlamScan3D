package com.yourorg.objectcapture.colmap

import java.io.File

class PoseFileWriter {
    fun write(file: File, images: List<ColmapImage>, cameraId: Int) {
        val builder = StringBuilder()
        builder.append("# Image list with two lines of data per image:\n")
        builder.append("# IMAGE_ID, QW, QX, QY, QZ, TX, TY, TZ, CAMERA_ID, IMAGE_NAME\n")
        builder.append("# Number of images: ${images.size}\n")

        images.sortedBy { it.id }.forEach { image ->
            val p = image.pose
            builder.append(
                "${image.id} ${p.qw} ${p.qx} ${p.qy} ${p.qz} ${p.tx} ${p.ty} ${p.tz} $cameraId ${image.name}\n"
            )
            builder.append("\n")
        }

        file.writeText(builder.toString())
    }
}
