package com.yourorg.objectcapture.ar

import com.google.ar.core.Pose
import kotlin.math.sqrt

object PoseUtils {
    data class ColmapPose(
        val qw: Double,
        val qx: Double,
        val qy: Double,
        val qz: Double,
        val tx: Double,
        val ty: Double,
        val tz: Double
    )

    fun arPoseToColmap(pose: Pose): ColmapPose {
        val matrix = FloatArray(16)
        pose.toMatrix(matrix, 0)
        val rCw = floatArrayOf(
            matrix[0], matrix[1], matrix[2],
            matrix[4], matrix[5], matrix[6],
            matrix[8], matrix[9], matrix[10]
        )
        val tCw = pose.translation
        return colmapFromRotationTranslation(rCw, tCw)
    }

    fun colmapFromRotationTranslation(rCw: FloatArray, tCw: FloatArray): ColmapPose {
        val rWc = transpose3x3(rCw)
        val tWc = multiplyMatrixVector(rWc, tCw).map { -it }
        val q = rotationMatrixToQuaternion(rWc)
        return ColmapPose(
            qw = q.w,
            qx = q.x,
            qy = q.y,
            qz = q.z,
            tx = tWc[0],
            ty = tWc[1],
            tz = tWc[2]
        )
    }

    fun rotationMatrixToQuaternion(r: FloatArray): Quaternion {
        val m00 = r[0]; val m01 = r[1]; val m02 = r[2]
        val m10 = r[3]; val m11 = r[4]; val m12 = r[5]
        val m20 = r[6]; val m21 = r[7]; val m22 = r[8]

        val trace = m00 + m11 + m22
        val (w, x, y, z) = if (trace > 0f) {
            val s = sqrt(trace + 1.0).toFloat() * 2f
            val w = 0.25f * s
            val x = (m21 - m12) / s
            val y = (m02 - m20) / s
            val z = (m10 - m01) / s
            floatArrayOf(w, x, y, z)
        } else if (m00 > m11 && m00 > m22) {
            val s = sqrt(1.0 + m00 - m11 - m22).toFloat() * 2f
            val w = (m21 - m12) / s
            val x = 0.25f * s
            val y = (m01 + m10) / s
            val z = (m02 + m20) / s
            floatArrayOf(w, x, y, z)
        } else if (m11 > m22) {
            val s = sqrt(1.0 + m11 - m00 - m22).toFloat() * 2f
            val w = (m02 - m20) / s
            val x = (m01 + m10) / s
            val y = 0.25f * s
            val z = (m12 + m21) / s
            floatArrayOf(w, x, y, z)
        } else {
            val s = sqrt(1.0 + m22 - m00 - m11).toFloat() * 2f
            val w = (m10 - m01) / s
            val x = (m02 + m20) / s
            val y = (m12 + m21) / s
            val z = 0.25f * s
            floatArrayOf(w, x, y, z)
        }

        return Quaternion(w.toDouble(), x.toDouble(), y.toDouble(), z.toDouble()).normalized()
    }

    fun transpose3x3(r: FloatArray): FloatArray {
        return floatArrayOf(
            r[0], r[3], r[6],
            r[1], r[4], r[7],
            r[2], r[5], r[8]
        )
    }

    fun multiplyMatrixVector(r: FloatArray, t: FloatArray): DoubleArray {
        return doubleArrayOf(
            (r[0] * t[0] + r[1] * t[1] + r[2] * t[2]).toDouble(),
            (r[3] * t[0] + r[4] * t[1] + r[5] * t[2]).toDouble(),
            (r[6] * t[0] + r[7] * t[1] + r[8] * t[2]).toDouble()
        )
    }

    fun poseMatrix4x4(pose: Pose): List<List<Double>> {
        val matrix = FloatArray(16)
        pose.toMatrix(matrix, 0)
        return listOf(
            listOf(matrix[0].toDouble(), matrix[1].toDouble(), matrix[2].toDouble(), matrix[3].toDouble()),
            listOf(matrix[4].toDouble(), matrix[5].toDouble(), matrix[6].toDouble(), matrix[7].toDouble()),
            listOf(matrix[8].toDouble(), matrix[9].toDouble(), matrix[10].toDouble(), matrix[11].toDouble()),
            listOf(matrix[12].toDouble(), matrix[13].toDouble(), matrix[14].toDouble(), matrix[15].toDouble())
        )
    }
}

data class Quaternion(
    val w: Double,
    val x: Double,
    val y: Double,
    val z: Double
) {
    fun normalized(): Quaternion {
        val norm = sqrt(w * w + x * x + y * y + z * z)
        if (norm == 0.0) return this
        return Quaternion(w / norm, x / norm, y / norm, z / norm)
    }
}
