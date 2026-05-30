package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ImmersiveSecondary,
    onPrimary = ImmersiveOnSecondary,
    primaryContainer = ImmersivePrimaryContainer,
    onPrimaryContainer = ImmersiveOnPrimaryContainer,
    secondary = ImmersiveSecondary,
    onSecondary = ImmersiveOnSecondary,
    background = ImmersiveOnSurface,
    surface = ImmersiveOnSurface,
    onBackground = ImmersiveBackground,
    onSurface = ImmersiveBackground,
    outline = ImmersiveOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    primaryContainer = ImmersivePrimaryContainer,
    onPrimaryContainer = ImmersiveOnPrimaryContainer,
    secondary = ImmersiveSecondary,
    onSecondary = ImmersiveOnSecondary,
    background = ImmersiveBackground,
    surface = ImmersiveSurface,
    onBackground = ImmersiveOnSurface,
    onSurface = ImmersiveOnSurface,
    onSurfaceVariant = ImmersiveOnSurfaceVariant,
    outline = ImmersiveOutline,
    surfaceVariant = ImmersiveSurfaceVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
