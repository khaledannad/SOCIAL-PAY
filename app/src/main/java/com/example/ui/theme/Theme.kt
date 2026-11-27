package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SocialPayDarkColorScheme = darkColorScheme(
  primary = BrandPrimaryAccent,
  onPrimary = BrandPrimaryBg,
  primaryContainer = BrandSurfaceVariant,
  onPrimaryContainer = BrandPrimaryAccent,
  secondary = BrandSecondaryAccent,
  onSecondary = BrandPrimaryBg,
  secondaryContainer = BrandSurfaceVariant,
  onSecondaryContainer = BrandSecondaryAccent,
  tertiary = BrandIntelligenceAccent,
  onTertiary = BrandPrimaryBg,
  background = BrandPrimaryBg,
  onBackground = BrandTextPrimary,
  surface = BrandSecondaryBg,
  onSurface = BrandTextPrimary,
  surfaceVariant = BrandSurfaceVariant,
  onSurfaceVariant = BrandTextSecondary,
  outline = BrandSurfaceBorder
)

private val SocialPayLightColorScheme = lightColorScheme(
  primary = Color(0xFF0D9488),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFCCFBF1),
  onPrimaryContainer = Color(0xFF115E59),
  secondary = Color(0xFF0284C7),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFE0F2FE),
  onSecondaryContainer = Color(0xFF0369A1),
  tertiary = Color(0xFF7C3AED),
  onTertiary = Color(0xFFFFFFFF),
  background = Color(0xFFF0F4F8),
  onBackground = Color(0xFF071923),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF071923),
  surfaceVariant = Color(0xFFE2E8F0),
  onSurfaceVariant = Color(0xFF475569),
  outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to unified dark futuristic identity
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) SocialPayDarkColorScheme else SocialPayLightColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
