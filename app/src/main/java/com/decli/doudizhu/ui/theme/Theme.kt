package com.decli.doudizhu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFE0BA67),
    onPrimary = Color(0xFF11221D),
    secondary = Color(0xFF4E7D73),
    background = Color(0xFF062A24),
    surface = Color(0xFF124238),
    onSurface = Color(0xFFF7F1E3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE0BA67),
    onPrimary = Color(0xFF14231F),
    secondary = Color(0xFF81B3A8),
    background = Color(0xFF031A16),
    surface = Color(0xFF0C342D),
    onSurface = Color(0xFFFFF7EA),
)

@Composable
fun DouDiZhuTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = DouDiZhuTypography,
        content = content,
    )
}

