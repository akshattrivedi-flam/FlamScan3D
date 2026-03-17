package com.yourorg.objectcapture.tests

import com.yourorg.objectcapture.ar.PoseUtils
import com.yourorg.objectcapture.colmap.CameraFileWriter
import com.yourorg.objectcapture.colmap.CameraIntrinsics
import com.yourorg.objectcapture.colmap.ColmapExporter
import com.yourorg.objectcapture.colmap.ColmapImage
import com.yourorg.objectcapture.colmap.ImageListWriter
import com.yourorg.objectcapture.colmap.PoseFileWriter
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ColmapExporterTest {
    @Test
    fun exporterWritesFiles() {
        val tmp = createTempDir()
        val exporter = ColmapExporter(CameraFileWriter(), PoseFileWriter(), ImageListWriter())
        val intrinsics = CameraIntrinsics(4032, 3024, 2800.0, 2795.0, 2016.0, 1512.0)
        val images = listOf(
            ColmapImage(1, "Images/frame_000001.jpg", PoseUtils.ColmapPose(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        )

        exporter.export(tmp, intrinsics, images)

        assertTrue(File(tmp, "cameras.txt").exists())
        assertTrue(File(tmp, "images.txt").exists())
        assertTrue(File(tmp, "image_list.txt").exists())
        assertTrue(File(tmp, "points3D.txt").exists())
    }
}
