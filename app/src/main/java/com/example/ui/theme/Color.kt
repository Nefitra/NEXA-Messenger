package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ActiveColors {
  var background by mutableStateOf(Color(0xFF050B18))
  var surface by mutableStateOf(Color(0xFF0A1229))
  var chatBubbleSelf by mutableStateOf(Color(0xFF1E1E38))
  var chatBubbleOther by mutableStateOf(Color(0xFF0E1A35))
  var cyberBlue by mutableStateOf(Color(0xFF22D3EE))
  var cyberPurple by mutableStateOf(Color(0xFF6366F1))
  var neonCyan by mutableStateOf(Color(0xFF00E5FF))
  var textPrimary by mutableStateOf(Color(0xFFF1F5F9))
  var textSecondary by mutableStateOf(Color(0xFF94A3B8))
  var tokenGreen by mutableStateOf(Color(0xFF10B981))
  var errorRed by mutableStateOf(Color(0xFFEF4444))
  var goldAccent by mutableStateOf(Color(0xFFFBBF24))
  var glassOverlay by mutableStateOf(Color(0x1BFFFFFF))
}

// Premium Dark Theme Palette - Geometric Balance
val MidnightBlue: Color get() = ActiveColors.background       // Deep Blue-Black Background
val DarkSlate: Color get() = ActiveColors.surface          // Bottom Nav & Secondary Background
val ChatBubbleSelf: Color get() = ActiveColors.chatBubbleSelf     // Indigo-ish Chat Bubble
val ChatBubbleOther: Color get() = ActiveColors.chatBubbleOther    // Slate-ish Chat Bubble

// Geometric Accents
val CyberBlue: Color get() = ActiveColors.cyberBlue          // Cyan-400 primary accent
val CyberPurple: Color get() = ActiveColors.cyberPurple        // Indigo Cyber purple
val NeonCyan: Color get() = ActiveColors.neonCyan           // Glowing online/spotlight cyan
val GoldAccent: Color get() = ActiveColors.goldAccent         // Warm amber indicators

// Text & Statuses
val TextPrimary: Color get() = ActiveColors.textPrimary        // Slate-100 Light Text
val TextSecondary: Color get() = ActiveColors.textSecondary      // Slate-400 Muted Text
val TokenGreen: Color get() = ActiveColors.tokenGreen         // Emerald Positive balance changes
val ErrorRed: Color get() = ActiveColors.errorRed           // Rose/Red Negative errors
val GlassOverlay: Color get() = ActiveColors.glassOverlay       // Glass borders or lines

