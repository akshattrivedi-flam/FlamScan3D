package com.yourorg.objectcapture.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CaptureMetricsStore {
    private val _depthGuidance = MutableStateFlow(DepthGuidance())
    val depthGuidance: StateFlow<DepthGuidance> = _depthGuidance.asStateFlow()

    private val _sessionScore = MutableStateFlow(SessionScore(0.0, 0f, 0.0, "", false))
    val sessionScore: StateFlow<SessionScore> = _sessionScore.asStateFlow()

    private val _orbitSuggestion = MutableStateFlow(OrbitSuggestion())
    val orbitSuggestion: StateFlow<OrbitSuggestion> = _orbitSuggestion.asStateFlow()

    fun updateDepth(guidance: DepthGuidance) {
        _depthGuidance.value = guidance
    }

    fun updateSessionScore(score: SessionScore) {
        _sessionScore.value = score
    }

    fun updateOrbitSuggestion(suggestion: OrbitSuggestion) {
        _orbitSuggestion.value = suggestion
    }

    fun reset() {
        _depthGuidance.value = DepthGuidance()
        _sessionScore.value = SessionScore(0.0, 0f, 0.0, "", false)
        _orbitSuggestion.value = OrbitSuggestion()
    }
}

data class DepthGuidance(
    val distanceMeters: Double = 0.0,
    val inRange: Boolean = true,
    val message: String = ""
)

data class OrbitSuggestion(
    val bandLabel: String = "",
    val message: String = ""
)
