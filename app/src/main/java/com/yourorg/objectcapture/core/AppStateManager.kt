package com.yourorg.objectcapture.core

import com.yourorg.objectcapture.capture.CaptureController
import com.yourorg.objectcapture.capture.CaptureSession
import com.yourorg.objectcapture.model.CaptureState
import com.yourorg.objectcapture.storage.CaptureFolderManager
import com.yourorg.objectcapture.storage.DraftManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateManager @Inject constructor(
    private val captureController: CaptureController,
    private val captureFolderManager: CaptureFolderManager,
    private val orbitManager: OrbitManager,
    private val feedbackManager: FeedbackManager,
    private val sessionScoreManager: SessionScoreManager,
    private val captureMetricsStore: CaptureMetricsStore,
    private val draftManager: DraftManager
) {
    private val _state = MutableStateFlow(CaptureState.READY)
    val state: StateFlow<CaptureState> = _state.asStateFlow()

    private var currentSession: CaptureSession? = null

    fun startNewSession() {
        if (_state.value != CaptureState.READY) return
        currentSession = captureFolderManager.createNewSession()
        orbitManager.reset()
        sessionScoreManager.reset()
        captureMetricsStore.reset()
        feedbackManager.clear()
        captureController.start(currentSession!!)
        _state.value = CaptureState.CAPTURING
    }

    fun resumeDraft() {
        if (_state.value != CaptureState.READY) return
        val draft = draftManager.findLatestDraft(captureFolderManager.getCapturesRoot()) ?: return
        val sessionRoot = java.io.File(draft.sessionPath)
        currentSession = captureFolderManager.openSession(sessionRoot)
        orbitManager.restoreCoverage(draft.coverageBins)
        sessionScoreManager.reset()
        captureMetricsStore.reset()
        feedbackManager.postInfo("Draft resumed")
        captureController.applyDraft(draft)
        captureController.start(currentSession!!)
        _state.value = CaptureState.CAPTURING
    }

    fun stopCaptureAndReview() {
        if (_state.value != CaptureState.CAPTURING) return
        captureController.stop()
        _state.value = CaptureState.REVIEWING
    }

    fun prepareReconstruction() {
        if (_state.value != CaptureState.REVIEWING) return
        _state.value = CaptureState.PREPARE_RECONSTRUCTION
    }

    fun startReconstruction() {
        if (_state.value != CaptureState.PREPARE_RECONSTRUCTION) return
        _state.value = CaptureState.RECONSTRUCTING
    }

    fun setViewingModel() {
        _state.value = CaptureState.VIEWING_MODEL
    }

    fun complete() {
        _state.value = CaptureState.COMPLETED
    }

    fun fail(errorMessage: String) {
        feedbackManager.postError(errorMessage)
        _state.value = CaptureState.FAILED
    }

    fun getCurrentSession(): CaptureSession? = currentSession
}
