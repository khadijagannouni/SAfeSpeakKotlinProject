package com.safespeak.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Ink900,
    onPrimary = Lime,
    secondary = Lime,
    onSecondary = Ink900,
    tertiary = AccentToxic,
    background = Paper,
    onBackground = Ink900,
    surface = Paper,
    onSurface = Ink900,
    surfaceVariant = Paper2,
    onSurfaceVariant = Ink700,
    outline = Ink500,
    error = AccentToxic
)

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = Ink900,
    secondary = Lime,
    onSecondary = Ink900,
    tertiary = AccentToxic,
    background = Ink900,
    onBackground = Paper,
    surface = Ink800,
    onSurface = Paper,
    surfaceVariant = Ink700,
    onSurfaceVariant = Paper2,
    outline = Ink500,
    error = AccentToxic
)

@Composable
fun SafeSpeakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = SafeSpeakTypography,
        content = content
    )
}
