package com.yourorg.objectcapture.core

import com.yourorg.objectcapture.model.Vector3
import kotlin.math.asin
import kotlin.math.atan2

class OrbitManager(
    private val bins: Int = 36,
    private val minCountPerBin: Int = 1
) {
    private val coverage = IntArray(bins)
    private val bandCoverage = arrayOf(IntArray(bins), IntArray(bins), IntArray(bins))
    private var objectCenter: Vector3? = null
    private var lastSuggestedBand: Int? = null

    private val bands = listOf(
        Band("Low", -30f..10f),
        Band("Mid", 10f..40f),
        Band("High", 40f..80f)
    )

    fun setObjectCenter(center: Vector3) {
        objectCenter = center
    }

    fun updateCoverage(cameraPosition: Vector3) {
        // Default to world origin so orbit tracking works without AR hit-testing
        val center = objectCenter ?: Vector3(0f, 0f, 0f)
        val dx = cameraPosition.x - center.x
        val dz = cameraPosition.z - center.z
        val dy = cameraPosition.y - center.y
        val distance = (cameraPosition - center).length()
        if (distance <= 0f) return

        val azimuth = Math.toDegrees(atan2(dz.toDouble(), dx.toDouble())).toFloat()
        val elevation = Math.toDegrees(asin((dy / distance).toDouble())).toFloat()
        val binIndex = ((azimuth + 180f) / 360f * bins).toInt().coerceIn(0, bins - 1)
        coverage[binIndex] += 1

        val bandIndex = bands.indexOfFirst { elevation in it.elevationRange }
        if (bandIndex >= 0) {
            bandCoverage[bandIndex][binIndex] += 1
        }
    }

    fun coverageRatio(): Float {
        val covered = coverage.count { it >= minCountPerBin }
        return covered.toFloat() / bins.toFloat()
    }

    fun getCoverageBins(): IntArray = coverage.copyOf()

    fun restoreCoverage(bins: List<Int>) {
        val limit = minOf(bins.size, coverage.size)
        for (i in 0 until limit) {
            coverage[i] = bins[i]
        }
        for (band in bandCoverage) {
            for (i in band.indices) band[i] = 0
        }
        lastSuggestedBand = null
    }

    fun recommendedBandSuggestion(): OrbitSuggestion {
        val bandIndex = bands.indices.minByOrNull { idx ->
            val covered = bandCoverage[idx].count { it >= minCountPerBin }
            covered
        } ?: return OrbitSuggestion()

        val band = bands[bandIndex]
        val message = if (lastSuggestedBand != bandIndex) {
            lastSuggestedBand = bandIndex
            "Capture missing sectors at ${band.label.lowercase()} elevation"
        } else {
            ""
        }

        return OrbitSuggestion(bandLabel = band.label, message = message)
    }

    fun reset() {
        for (i in coverage.indices) coverage[i] = 0
        for (band in bandCoverage) {
            for (i in band.indices) band[i] = 0
        }
        objectCenter = null
        lastSuggestedBand = null
    }

    private data class Band(val label: String, val elevationRange: ClosedFloatingPointRange<Float>)
}
