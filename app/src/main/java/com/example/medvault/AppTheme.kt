package com.example.medvault

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppDarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFE2ECEA),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFE2ECEA),
    surfaceVariant = Color(0xFF18221F),
    onSurfaceVariant = Color(0xFFB9C7C4),
    outline = Color(0xFF6B7C79),
    outlineVariant = Color(0xFF2B3836),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2D0000),
    errorContainer = Color(0xFF4A1414),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val AppLightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    background = Color(0xFFF2F6F5),
    onBackground = Color(0xFF14201E),
    surface = Color(0xFFF2F6F5),
    onSurface = Color(0xFF14201E),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF40504D),
    outline = Color(0xFF8A9A97),
    outlineVariant = Color(0xFFD3DDDB),
    error = Color(0xFFD32F2F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) AppDarkColors else AppLightColors,
        shapes = AppShapes,
        content = content
    )
}