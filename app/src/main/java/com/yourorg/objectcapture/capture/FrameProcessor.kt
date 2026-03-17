package com.yourorg.objectcapture.capture

import kotlin.math.abs

class FrameProcessor(
    private val frameSelector: FrameSelector = FrameSelector()
) {
    fun processFrame(sample: FrameSample): FrameSelectionResult {
        val sharpness = computeSharpness(sample.grayscale, sample.width, sample.height)
        val metrics = FrameMetrics(
            poseDeltaDegrees = sample.poseDeltaDegrees,
            translationDeltaMeters = sample.translationDeltaMeters,
            sharpness = sharpness,
            depthMean = sample.depthMean,
            depthStdDev = sample.depthStdDev,
            exposureDelta = sample.exposureDelta,
            trackingState = sample.trackingState,
            featureCount = sample.featureCount
        )
        val result = frameSelector.evaluate(metrics)
        return result.copy(sharpness = sharpness)
    }

    private fun computeSharpness(luma: ByteArray, width: Int, height: Int): Double {
        if (luma.isEmpty() || width <= 2 || height <= 2) return 0.0
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        val get = { x: Int, y: Int -> (luma[y * width + x].toInt() and 0xFF).toDouble() }
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = get(x, y)
                val lap =
                    -4 * center +
                    get(x - 1, y) + get(x + 1, y) +
                    get(x, y - 1) + get(x, y + 1)
                sum += lap
                sumSq += lap * lap
                count++
            }
        }
        val mean = sum / count
        val variance = (sumSq / count) - mean * mean
        return variance
    }
}

data class FrameSample(
    val grayscale: ByteArray,
    val width: Int,
    val height: Int,
    val poseDeltaDegrees: Double,
    val translationDeltaMeters: Double,
    val depthMean: Double,
    val depthStdDev: Double,
    val exposureDelta: Double,
    val trackingState: TrackingState,
    val featureCount: Int
)

data class FrameMetrics(
    val poseDeltaDegrees: Double,
    val translationDeltaMeters: Double,
    val sharpness: Double,
    val depthMean: Double,
    val depthStdDev: Double,
    val exposureDelta: Double,
    val trackingState: TrackingState,
    val featureCount: Int
)

enum class TrackingState {
    TRACKING,
    PAUSED,
    STOPPED
}

data class FrameSelectionResult(
    val score: Double,
    val accepted: Boolean,
    val reasons: List<String>,
    val sharpness: Double = 0.0
)

class FrameSelector(
    private val sharpnessThreshold: Double = 50.0,
    private val minFeatureCount: Int = 200,
    private val minAngleDelta: Double = 5.0,
    private val minTranslationDelta: Double = 0.05,
    private val depthMinMeters: Double = 0.25,
    private val depthMaxMeters: Double = 1.5,
    private val maxDepthStdDev: Double = 0.08,
    private val maxExposureDelta: Double = 0.25,
    private val weights: Weights = Weights()
) {
    data class Weights(
        val pose: Double = 0.35,
        val sharpness: Double = 0.25,
        val features: Double = 0.2,
        val depth: Double = 0.15,
        val tracking: Double = 0.05
    )

    fun evaluate(metrics: FrameMetrics): FrameSelectionResult {
        val reasons = mutableListOf<String>()

        val poseOk = metrics.poseDeltaDegrees >= minAngleDelta ||
            metrics.translationDeltaMeters >= minTranslationDelta
        if (!poseOk) reasons.add("Pose delta too small")

        val sharpnessOk = metrics.sharpness >= sharpnessThreshold
        if (!sharpnessOk) reasons.add("Low sharpness")

        val depthOk = metrics.depthMean in depthMinMeters..depthMaxMeters &&
            metrics.depthStdDev <= maxDepthStdDev
        if (!depthOk) reasons.add("Depth out of range or unstable")

        val exposureOk = abs(metrics.exposureDelta) <= maxExposureDelta
        if (!exposureOk) reasons.add("Exposure jump")

        val trackingOk = metrics.trackingState == TrackingState.TRACKING
        if (!trackingOk) reasons.add("Not tracking")

        val featuresOk = metrics.featureCount >= minFeatureCount
        if (!featuresOk) reasons.add("Low feature count")

        val poseScore = normalize(metrics.poseDeltaDegrees, minAngleDelta, 20.0)
        val sharpnessScore = normalize(metrics.sharpness, sharpnessThreshold, 200.0)
        val featuresScore = normalize(metrics.featureCount.toDouble(), minFeatureCount.toDouble(), 600.0)
        val depthScore = if (depthOk) 1.0 else 0.0
        val trackingScore = if (trackingOk) 1.0 else 0.0

        val score = weights.pose * poseScore +
            weights.sharpness * sharpnessScore +
            weights.features * featuresScore +
            weights.depth * depthScore +
            weights.tracking * trackingScore

        val accepted = score > 0.5 && poseOk && sharpnessOk && depthOk && exposureOk && trackingOk && featuresOk
        return FrameSelectionResult(score = score, accepted = accepted, reasons = reasons, sharpness = metrics.sharpness)
    }

    private fun normalize(value: Double, min: Double, max: Double): Double {
        if (max <= min) return 0.0
        val clamped = value.coerceIn(min, max)
        return (clamped - min) / (max - min)
    }
}
