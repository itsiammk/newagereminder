package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = CyanAccent,
    secondary = PurpleAccent,
    tertiary = EmeraldAccent,
    background = CosmicDark,
    surface = CosmicSurface,
    onBackground = Color(0xFFE2E8F0), // Slate 100
    onSurface = Color(0xFFF1F5F9), // Slate 200
    primaryContainer = Color(0xFF0891B2).copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFFE2F9FF)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
