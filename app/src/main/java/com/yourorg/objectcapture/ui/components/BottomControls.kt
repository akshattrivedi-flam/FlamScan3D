package com.yourorg.objectcapture.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
fun BottomControls(
    captureState: CaptureState,
    markerActive: Boolean,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onManualCapture: () -> Unit,
    onSetMarker: () -> Unit,
    onClearMarker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val capturing = captureState == CaptureState.CAPTURING

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = if (capturing) onStopCapture else onStartCapture,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    if (capturing) "Stop Capture" else "Start Capture",
                    fontWeight = FontWeight.SemiBold
                )
            }
            FilledTonalButton(
                onClick = onManualCapture,
                enabled = capturing,
                modifier = Modifier.weight(1f)
            ) {
                Text("Manual Frame")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = if (markerActive) onClearMarker else onSetMarker,
                enabled = capturing,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (markerActive) "Clear Marker" else "Set Marker")
            }
            Text(
                text = if (capturing) "Keep a full orbit" else "Ready",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
