package com.yourorg.objectcapture.tests

import com.yourorg.objectcapture.ar.PoseUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class PoseUtilsTest {
    @Test
    fun colmapPoseReconstructsCameraCenter() {
        val yawDeg = 90.0
        val yawRad = Math.toRadians(yawDeg)
        val c = cos(yawRad).toFloat()
        val s = sin(yawRad).toFloat()

        // Rotation around Y axis (camera -> world)
        val rCw = floatArrayOf(
            c, 0f, s,
            0f, 1f, 0f,
            -s, 0f, c
        )
        val tCw = floatArrayOf(0.5f, 0.0f, 0.0f)

        val colmap = PoseUtils.colmapFromRotationTranslation(rCw, tCw)

        // Reconstruct camera center from COLMAP extrinsics: C = -R_cw * T_wc
        val rWc = PoseUtils.transpose3x3(rCw)
        val tWc = doubleArrayOf(colmap.tx, colmap.ty, colmap.tz)

        val cX = -(rCw[0] * tWc[0] + rCw[1] * tWc[1] + rCw[2] * tWc[2])
        val cY = -(rCw[3] * tWc[0] + rCw[4] * tWc[1] + rCw[5] * tWc[2])
        val cZ = -(rCw[6] * tWc[0] + rCw[7] * tWc[1] + rCw[8] * tWc[2])

        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.0), doubleArrayOf(cX, cY, cZ), 1e-6)
        assertEquals(1.0, Math.sqrt(colmap.qw * colmap.qw + colmap.qx * colmap.qx + colmap.qy * colmap.qy + colmap.qz * colmap.qz), 1e-6)
    }
}
