package com.example.donor.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary        = RedPrimary,
    onPrimary      = White,
    secondary      = RedLight,
    background     = BackgroundLight,
    surface        = SurfaceCard,
    onBackground   = Color(0xFF1C1C1C),
    onSurface      = Color(0xFF1C1C1C),
)

private val DarkColorScheme = darkColorScheme(
    primary        = RedLight,
    onPrimary      = Color(0xFF1C1C1C),
    secondary      = RedDark,
    background     = Color(0xFF121212),
    surface        = Color(0xFF1E1E1E),
    onBackground   = White,
    onSurface      = White,
)

@Composable
fun DonorTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}