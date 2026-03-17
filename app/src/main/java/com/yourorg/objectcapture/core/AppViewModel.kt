package com.yourorg.objectcapture.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourorg.objectcapture.model.CaptureState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val appStateManager: AppStateManager,
    private val feedbackManager: FeedbackManager,
    private val orbitManager: OrbitManager,
    private val captureMetricsStore: CaptureMetricsStore
) : ViewModel() {

    val captureState: StateFlow<CaptureState> = appStateManager.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, CaptureState.READY)

    val messages = feedbackManager.messages
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val depthGuidance = captureMetricsStore.depthGuidance
        .stateIn(viewModelScope, SharingStarted.Eagerly, DepthGuidance())

    val sessionScore = captureMetricsStore.sessionScore
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionScore(0.0, 0f, 0.0, "", false))

    fun startCapture() = appStateManager.startNewSession()
    fun resumeDraft() = appStateManager.resumeDraft()
    fun stopAndReview() = appStateManager.stopCaptureAndReview()
    fun prepareReconstruction() = appStateManager.prepareReconstruction()
    fun startReconstruction() = appStateManager.startReconstruction()
    fun setViewingModel() = appStateManager.setViewingModel()
    fun complete() = appStateManager.complete()
    fun getCoverageBins(): IntArray = orbitManager.getCoverageBins()
    fun getCurrentSession() = appStateManager.getCurrentSession()
}
