package com.yourorg.objectcapture.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun OrbitGuide(bins: IntArray) {
    if (bins.isEmpty()) return

    val max = bins.maxOrNull()?.coerceAtLeast(1) ?: 1
    val segmentCount = bins.size
    val gapDegrees = 2f
    val sweep = (360f / segmentCount) - gapDegrees

    Box(modifier = Modifier.padding(16.dp)) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val stroke = Stroke(width = 10f, cap = StrokeCap.Round)
            bins.forEachIndexed { index, count ->
                val intensity = (count.toFloat() / max).coerceIn(0f, 1f)
                val color = if (count == 0) {
                    Color(0xFFD32F2F)
                } else {
                    Color(1f - intensity, intensity, 0f, 0.9f)
                }
                val startAngle = index * (360f / segmentCount)
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = stroke
                )
            }
        }
    }
}
