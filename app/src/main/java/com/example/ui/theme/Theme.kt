package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
  themeMode: String = "cyber_midnight", // "cyber_midnight", "tg_dark", "tg_light"
  content: @Composable () -> Unit,
) {
  LaunchedEffect(themeMode) {
    when (themeMode) {
      "cyber_midnight" -> {
        ActiveColors.background = Color(0xFF050B18)
        ActiveColors.surface = Color(0xFF0A1229)
        ActiveColors.chatBubbleSelf = Color(0xFF1E1E38)
        ActiveColors.chatBubbleOther = Color(0xFF0E1A35)
        ActiveColors.cyberBlue = Color(0xFF22D3EE)
        ActiveColors.cyberPurple = Color(0xFF6366F1)
        ActiveColors.neonCyan = Color(0xFF00E5FF)
        ActiveColors.textPrimary = Color(0xFFF1F5F9)
        ActiveColors.textSecondary = Color(0xFF94A3B8)
        ActiveColors.tokenGreen = Color(0xFF10B981)
        ActiveColors.errorRed = Color(0xFFEF4444)
        ActiveColors.goldAccent = Color(0xFFFBBF24)
        ActiveColors.glassOverlay = Color(0x1BFFFFFF)
      }
      "tg_dark" -> {
        ActiveColors.background = Color(0xFF18222D)
        ActiveColors.surface = Color(0xFF1E2E3E)
        ActiveColors.chatBubbleSelf = Color(0xFF2B5278)
        ActiveColors.chatBubbleOther = Color(0xFF182533)
        ActiveColors.cyberBlue = Color(0xFF3390EC) // Telegram Native Blue
        ActiveColors.cyberPurple = Color(0xFF5970E1)
        ActiveColors.neonCyan = Color(0xFF50B2FF)
        ActiveColors.textPrimary = Color(0xFFFFFFFF)
        ActiveColors.textSecondary = Color(0xFF8B9BB4)
        ActiveColors.tokenGreen = Color(0xFF10B981)
        ActiveColors.errorRed = Color(0xFFEF4444)
        ActiveColors.goldAccent = Color(0xFFF8C045)
        ActiveColors.glassOverlay = Color(0x15FFFFFF)
      }
      "tg_light" -> {
        ActiveColors.background = Color(0xFFF0F2F5)
        ActiveColors.surface = Color(0xFFFFFFFF)
        ActiveColors.chatBubbleSelf = Color(0xFFE2F7CB) // Classic light green
        ActiveColors.chatBubbleOther = Color(0xFFFFFFFF)
        ActiveColors.cyberBlue = Color(0xFF2481CC) // Telegram Classic Blue
        ActiveColors.cyberPurple = Color(0xFF6B4EE0)
        ActiveColors.neonCyan = Color(0xFF3390EC)
        ActiveColors.textPrimary = Color(0xFF000000)
        ActiveColors.textSecondary = Color(0xFF7A8A96)
        ActiveColors.tokenGreen = Color(0xFF0F9C59)
        ActiveColors.errorRed = Color(0xFFD32F2F)
        ActiveColors.goldAccent = Color(0xFFE0A000)
        ActiveColors.glassOverlay = Color(0x12000000)
      }
    }
  }

  val isLight = themeMode == "tg_light"
  val colorScheme = if (isLight) {
    lightColorScheme(
      primary = ActiveColors.cyberBlue,
      onPrimary = Color.White,
      background = ActiveColors.background,
      onBackground = ActiveColors.textPrimary,
      surface = ActiveColors.surface,
      onSurface = ActiveColors.textPrimary,
      error = ActiveColors.errorRed
    )
  } else {
    darkColorScheme(
      primary = ActiveColors.cyberBlue,
      onPrimary = Color.White,
      background = ActiveColors.background,
      onBackground = ActiveColors.textPrimary,
      surface = ActiveColors.surface,
      onSurface = ActiveColors.textPrimary,
      error = ActiveColors.errorRed
    )
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

