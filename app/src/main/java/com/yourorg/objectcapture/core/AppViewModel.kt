package com.yourorg.objectcapture.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourorg.objectcapture.model.CaptureState
import com.yourorg.objectcapture.network.JobStatusPoller
import com.yourorg.objectcapture.network.UploadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val appStateManager: AppStateManager,
    private val feedbackManager: FeedbackManager,
    private val orbitManager: OrbitManager,
    private val captureMetricsStore: CaptureMetricsStore,
    private val uploadManager: UploadManager,
    private val jobStatusPoller: JobStatusPoller
) : ViewModel() {

    val captureState: StateFlow<CaptureState> = appStateManager.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, CaptureState.READY)

    val messages = feedbackManager.messages
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val depthGuidance = captureMetricsStore.depthGuidance
        .stateIn(viewModelScope, SharingStarted.Eagerly, DepthGuidance())

    val sessionScore = captureMetricsStore.sessionScore
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionScore(0.0, 0f, 0.0, "", false))

    private val _uploadState = MutableStateFlow(UploadUiState())
    val uploadState: StateFlow<UploadUiState> = _uploadState

    fun startCapture() = appStateManager.startNewSession()
    fun resumeDraft() = appStateManager.resumeDraft()
    fun stopAndReview() = appStateManager.stopCaptureAndReview()
    fun prepareReconstruction() = appStateManager.prepareReconstruction()
    fun startReconstruction() = appStateManager.startReconstruction()
    fun setViewingModel() = appStateManager.setViewingModel()
    fun complete() = appStateManager.complete()
    fun getCoverageBins(): IntArray = orbitManager.getCoverageBins()
    fun getCurrentSession() = appStateManager.getCurrentSession()

    fun startUpload() {
        val session = appStateManager.getCurrentSession() ?: return
        viewModelScope.launch {
            _uploadState.value = UploadUiState(inProgress = true, progress = 0)
            try {
                val jobId = uploadManager.uploadSession(session) { progress ->
                    _uploadState.update { it.copy(progress = progress.coerceIn(0, 100)) }
                }
                _uploadState.update { it.copy(jobId = jobId, status = "processing") }
                jobStatusPoller.poll(jobId).collect { status ->
                    _uploadState.update {
                        it.copy(
                            status = status.status,
                            progress = status.progress.coerceIn(0, 100),
                            modelUrl = status.model_url
                        )
                    }
                }
            } catch (t: Throwable) {
                _uploadState.value = UploadUiState(inProgress = false, error = t.message ?: "Upload failed")
            }
        }
    }
}
