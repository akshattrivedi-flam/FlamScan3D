package com.yourorg.objectcapture.core

class SessionScoreManager {
    private var count: Int = 0
    private var meanScore: Double = 0.0
    private var lastGuidance: String? = null

    fun update(frameScore: Double, orbitCoverage: Float): SessionScore {
        count += 1
        meanScore += (frameScore - meanScore) / count.toDouble()
        val sessionScore = 0.7 * meanScore + 0.3 * orbitCoverage

        val guidance = when {
            sessionScore < 0.45 -> "Poor scan — move slower & increase coverage"
            sessionScore < 0.7 -> "Fair — capture more angles"
            else -> "Good — ready to reconstruct"
        }

        val changed = guidance != lastGuidance
        if (changed) {
            lastGuidance = guidance
        }

        return SessionScore(
            meanFrameScore = meanScore,
            orbitCoverage = orbitCoverage,
            sessionScore = sessionScore,
            guidance = guidance,
            guidanceChanged = changed
        )
    }

    fun reset() {
        count = 0
        meanScore = 0.0
        lastGuidance = null
    }
}

data class SessionScore(
    val meanFrameScore: Double,
    val orbitCoverage: Float,
    val sessionScore: Double,
    val guidance: String,
    val guidanceChanged: Boolean
)
