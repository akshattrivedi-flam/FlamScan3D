package com.yourorg.objectcapture.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
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

@Composable
fun CaptureScreen(viewModel: AppViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val entryPoint = EntryPointAccessors.fromApplication(context, CaptureEntryPoint::class.java)
    val cameraController: CameraController = entryPoint.cameraController()
    val captureController: CaptureController = entryPoint.captureController()

    val captureState by viewModel.captureState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val depthGuidance by viewModel.depthGuidance.collectAsState()
    val sessionScore by viewModel.sessionScore.collectAsState()

    val sizeState = remember { mutableStateOf(androidx.compose.ui.unit.IntSize(0, 0)) }

    val permissions = remember {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
    val hasPermissions = remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermissions.value = permissions.all { result[it] == true }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions.value) {
            permissionLauncher.launch(permissions)
        }
    }

    DisposableEffect(hasPermissions.value) {
        if (hasPermissions.value) {
            captureController.startPreview()
        }
        onDispose {
            captureController.stopPreview()
        }
    }

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
                    preview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    preview.scaleType = PreviewView.ScaleType.FILL_CENTER
                    cameraController.attachPreview(preview, lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        CaptureOverlay(
            captureState = captureState,
            markerActive = depthGuidance.markerActive,
            onStartCapture = { viewModel.startCapture() },
            onStopCapture = { viewModel.stopAndReview() },
            onManualCapture = { captureController.captureManual() },
            onSaveDraft = { captureController.saveDraft() },
            onResumeDraft = { viewModel.resumeDraft() },
            onSetMarker = { captureController.setDistanceMarker() },
            onClearMarker = { captureController.clearDistanceMarker() }
        )

        // Only show the orbit ring while actively capturing (avoids the red
        // full-circle that appears when all bins are zero in READY state)
        if (captureState == com.yourorg.objectcapture.model.CaptureState.CAPTURING) {
            OrbitGuide(
                bins = viewModel.getCoverageBins(),
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
        FeedbackBanner(messages = messages, modifier = Modifier.align(Alignment.TopEnd))
        SessionScoreBanner(score = sessionScore, modifier = Modifier.align(Alignment.TopCenter))
        DepthDistanceOverlay(
            guidance = depthGuidance,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 88.dp)
        )

        if (!hasPermissions.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Camera permission required",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Enable camera + microphone to start guided capture.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB7C3CC),
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                    Button(onClick = { permissionLauncher.launch(permissions) }) {
                        Text("Grant Permissions")
                    }
                }
            }
        }
    }
}
