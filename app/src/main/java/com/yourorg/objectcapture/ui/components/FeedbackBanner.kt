package com.yourorg.objectcapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourorg.objectcapture.model.FeedbackMessage

@Composable
fun FeedbackBanner(messages: List<FeedbackMessage>, modifier: Modifier = Modifier) {
    val latest = messages.lastOrNull() ?: return
    val color = when (latest.level) {
        FeedbackMessage.Level.INFO -> Color(0xFF222222)
        FeedbackMessage.Level.WARNING -> Color(0xFFFF9800)
        FeedbackMessage.Level.ERROR -> Color(0xFFD32F2F)
    }

    Box(modifier = modifier.padding(16.dp).background(color).padding(12.dp)) {
        Text(latest.text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}
