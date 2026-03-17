package com.yourorg.objectcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourorg.objectcapture.model.CaptureState

@Composable
fun CaptureOverlay(
    captureState: CaptureState,
    markerActive: Boolean,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onManualCapture: () -> Unit,
    onSaveDraft: () -> Unit,
    onResumeDraft: () -> Unit,
    onSetMarker: () -> Unit,
    onClearMarker: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xCC0B0F14),
                        1f to Color.Transparent
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color(0xCC0B0F14)
                    )
                )
        )

        TopControls(
            captureState = captureState,
            onReview = onStopCapture,
            onSaveDraft = onSaveDraft,
            onResumeDraft = onResumeDraft,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        BottomControls(
            captureState = captureState,
            markerActive = markerActive,
            onStartCapture = onStartCapture,
            onStopCapture = onStopCapture,
            onManualCapture = onManualCapture,
            onSetMarker = onSetMarker,
            onClearMarker = onClearMarker,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
