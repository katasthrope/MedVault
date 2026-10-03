package com.example.medvault

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppDarkColors = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF00201D),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFE6E6E6),
    outline = Color(0xFF8A8A8A),
    error = Color(0xFFFF5252)
)

private val AppLightColors = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1C1C),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF1C1C1C),
    outline = Color(0xFF757575),
    error = Color(0xFFB3261E)
)

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) AppDarkColors else AppLightColors,
        content = content
    )
}