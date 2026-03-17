package com.yourorg.objectcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourorg.objectcapture.core.SessionScore

@Composable
fun SessionScoreBanner(score: SessionScore, modifier: Modifier = Modifier) {
    if (score.guidance.isBlank()) return
    val color = when {
        score.sessionScore < 0.45 -> Color(0xFFD32F2F)
        score.sessionScore < 0.7 -> Color(0xFFF57C00)
        else -> Color(0xFF2E7D32)
    }
    Text(
        text = score.guidance,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        modifier = modifier
            .padding(16.dp)
            .background(color)
            .padding(10.dp)
    )
}
