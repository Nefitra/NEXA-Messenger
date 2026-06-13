package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberDarkColorScheme = darkColorScheme(
  primary = CyberBlue,
  onPrimary = Color.White,
  primaryContainer = ChatBubbleSelf,
  onPrimaryContainer = TextPrimary,
  secondary = CyberPurple,
  onSecondary = Color.White,
  tertiary = NeonCyan,
  background = MidnightBlue,
  onBackground = TextPrimary,
  surface = DarkSlate,
  onSurface = TextPrimary,
  surfaceVariant = Color(0xFF1A1F2C),
  onSurfaceVariant = TextSecondary,
  error = ErrorRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default as requested
  dynamicColor: Boolean = false, // Disable dynamic color to maintain NEXA's cyber-branding style
  content: @Composable () -> Unit,
) {
  val colorScheme = CyberDarkColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
