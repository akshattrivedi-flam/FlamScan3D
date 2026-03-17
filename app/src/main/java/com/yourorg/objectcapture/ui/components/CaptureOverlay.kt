package com.yourorg.objectcapture.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CaptureOverlay(
    onStartCapture: () -> Unit,
    onReview: () -> Unit,
    onManualCapture: () -> Unit,
    onSaveDraft: () -> Unit,
    onResumeDraft: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TopControls(
            onReview = onReview,
            onSaveDraft = onSaveDraft,
            onResumeDraft = onResumeDraft,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        BottomControls(onStartCapture = onStartCapture, onManualCapture = onManualCapture, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
