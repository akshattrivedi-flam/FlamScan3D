package com.yourorg.objectcapture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.camera.view.PreviewView
import com.yourorg.objectcapture.capture.CaptureController
import com.yourorg.objectcapture.camera.CameraController
import com.yourorg.objectcapture.core.AppViewModel
import com.yourorg.objectcapture.di.CaptureEntryPoint
import com.yourorg.objectcapture.ui.components.CaptureOverlay
import com.yourorg.objectcapture.ui.components.DepthDistanceOverlay
import com.yourorg.objectcapture.ui.components.FeedbackBanner
import com.yourorg.objectcapture.ui.components.OrbitGuide
import com.yourorg.objectcapture.ui.components.SessionScoreBanner
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.foundation.gestures.detectTapGestures

@Composable
fun CaptureScreen(viewModel: AppViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val entryPoint = EntryPointAccessors.fromApplication(context, CaptureEntryPoint::class.java)
    val cameraController: CameraController = entryPoint.cameraController()
    val captureController: CaptureController = entryPoint.captureController()
    val messages by viewModel.messages.collectAsState()
    val depthGuidance by viewModel.depthGuidance.collectAsState()
    val sessionScore by viewModel.sessionScore.collectAsState()

    val sizeState = remember { mutableStateOf(androidx.compose.ui.unit.IntSize(0, 0)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { sizeState.value = it }
            .pointerInput(Unit) {
                detectTapGestures { offset: Offset ->
                    val size = sizeState.value
                    if (size.width > 0 && size.height > 0) {
                        captureController.setObjectCenterFromTap(offset.x, offset.y, size.width, size.height)
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { preview ->
                    cameraController.attachPreview(preview, lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        CaptureOverlay(
            onStartCapture = { viewModel.startCapture() },
            onReview = { viewModel.stopAndReview() },
            onManualCapture = { captureController.captureManual() },
            onSaveDraft = { captureController.saveDraft() },
            onResumeDraft = { viewModel.resumeDraft() }
        )

        FeedbackBanner(messages = messages)
        SessionScoreBanner(score = sessionScore)
        OrbitGuide(bins = viewModel.getCoverageBins())
        DepthDistanceOverlay(guidance = depthGuidance)
    }

    LaunchedEffect(Unit) {
        captureController
    }
}
