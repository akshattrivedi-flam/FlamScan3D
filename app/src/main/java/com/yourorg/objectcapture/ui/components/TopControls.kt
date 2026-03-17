package com.yourorg.objectcapture.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourorg.objectcapture.model.CaptureState

@Composable
fun TopControls(
    captureState: CaptureState,
    onReview: () -> Unit,
    onSaveDraft: () -> Unit,
    onResumeDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val capturing = captureState == CaptureState.CAPTURING
    val ready = captureState == CaptureState.READY

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalButton(
            onClick = onReview,
            enabled = capturing,
            modifier = Modifier.weight(1f)
        ) {
            Text("Review", fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onSaveDraft,
            enabled = capturing,
            modifier = Modifier.weight(1f)
        ) {
            Text("Save Draft", color = MaterialTheme.colorScheme.onSurface)
        }
        OutlinedButton(
            onClick = onResumeDraft,
            enabled = ready,
            modifier = Modifier.weight(1f)
        ) {
            Text("Resume", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
