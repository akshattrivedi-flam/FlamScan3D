package com.yourorg.objectcapture.storage

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

class MetadataWriter {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(FrameMetadata::class.java)

    fun writeFrameMetadata(outputFile: File, metadata: FrameMetadata) {
        outputFile.writeText(adapter.indent("  ").toJson(metadata))
    }
}

data class FrameMetadata(
    val image: String,
    val timestamp_ms: Long,
    val pose: PoseMetadata,
    val intrinsics: IntrinsicsMetadata,
    val depth: DepthMetadata,
    val tracking_state: String,
    val sharpness: Double,
    val feature_count: Int,
    val frame_score: Double
)

data class PoseMetadata(
    val arcore_pose_matrix: List<List<Double>>,
    val colmap_q: List<Double>,
    val colmap_t: List<Double>
)

data class IntrinsicsMetadata(
    val width: Int,
    val height: Int,
    val fx: Double,
    val fy: Double,
    val cx: Double,
    val cy: Double,
    val distortion: List<Double>? = null
)

data class DepthMetadata(
    val median_m: Double,
    val mean_m: Double,
    val stddev_m: Double
)
