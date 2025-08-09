package com.example.puzzledrop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = GemBlue,
    secondary = GemPurple,
    background = BackgroundDark,
    surface = Color(0xFF171B22),
    onSurface = Color(0xFFE6EAF0),
)

private val LightColors = lightColorScheme(
    primary = GemBlue,
    secondary = GemPurple,
    background = BackgroundLight,
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1116),
)

@Composable
fun PuzzleDropTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}