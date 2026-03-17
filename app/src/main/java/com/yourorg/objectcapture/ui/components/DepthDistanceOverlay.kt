package com.yourorg.objectcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourorg.objectcapture.core.DepthGuidance

@Composable
fun DepthDistanceOverlay(guidance: DepthGuidance) {
    if (guidance.distanceMeters <= 0.0) return
    val normalized = (guidance.distanceMeters / 1.0).coerceIn(0.0, 1.0)
    val barColor = if (guidance.inRange) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color(0x66000000))
            .padding(12.dp)
    ) {
        Text(
            text = if (guidance.message.isNotEmpty()) guidance.message else "Distance OK",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        LinearProgressIndicator(
            progress = normalized.toFloat(),
            color = barColor,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Text(
            text = "${String.format("%.2f", guidance.distanceMeters)} m",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}
