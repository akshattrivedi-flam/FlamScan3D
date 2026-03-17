package com.yourorg.objectcapture.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
    primary = Color(0xFF00C2A8),
    onPrimary = Color(0xFF00332C),
    secondary = Color(0xFFFFB86B),
    onSecondary = Color(0xFF3B2600),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFE8EEF2),
    surface = Color(0xFF111821),
    onSurface = Color(0xFFE6EEF5),
    surfaceVariant = Color(0xFF1B2630),
    onSurfaceVariant = Color(0xFFB7C3CC),
    error = Color(0xFFE35B5B),
    onError = Color(0xFF2B0000)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00A48F),
    onPrimary = Color.White,
    secondary = Color(0xFFB35A00),
    onSecondary = Color.White,
    background = Color(0xFFF3F6F8),
    onBackground = Color(0xFF0C0F12),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111417),
    surfaceVariant = Color(0xFFE2E8ED),
    onSurfaceVariant = Color(0xFF3A434C),
    error = Color(0xFFB00020),
    onError = Color.White
)

private val AppTypography = Typography(
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)

@Composable
fun ObjectCaptureTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
