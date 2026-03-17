package com.yourorg.objectcapture.colmap

import com.yourorg.objectcapture.ar.PoseUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColmapExporter @Inject constructor(
    private val cameraFileWriter: CameraFileWriter,
    private val poseFileWriter: PoseFileWriter,
    private val imageListWriter: ImageListWriter
) {
    fun export(
        outputDir: File,
        intrinsics: CameraIntrinsics,
        images: List<ColmapImage>,
        cameraModel: String = "PINHOLE",
        cameraId: Int = 1
    ) {
        if (!outputDir.exists()) outputDir.mkdirs()
        val camerasFile = File(outputDir, "cameras.txt")
        val imagesFile = File(outputDir, "images.txt")
        val pointsFile = File(outputDir, "points3D.txt")
        val listFile = File(outputDir, "image_list.txt")

        cameraFileWriter.write(camerasFile, cameraId, cameraModel, intrinsics)
        poseFileWriter.write(imagesFile, images, cameraId)
        imageListWriter.write(listFile, images)

        if (!pointsFile.exists()) {
            pointsFile.writeText("# 3D point list is empty - placeholder\n")
        }
    }
}

data class CameraIntrinsics(
    val width: Int,
    val height: Int,
    val fx: Double,
    val fy: Double,
    val cx: Double,
    val cy: Double,
    val distortion: List<Double>? = null
)

data class ColmapImage(
    val id: Int,
    val name: String,
    val pose: PoseUtils.ColmapPose
)
