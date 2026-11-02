package com.rkbapps.tooai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun TooAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Icons contrast with the primary-colored top bars, not the surface.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content
    )
}

// Follows the scheme TooAiTheme picked, so the saved theme preference is respected.
val ColorScheme.isDark: Boolean
    get() = surface.luminance() < 0.5f

val MaterialTheme.toolPalette: ToolPalette
    @Composable get() = if (colorScheme.isDark) DarkToolPalette else LightToolPalette

// For screens without a primary-colored top bar. Restores the theme default on exit.
@Composable
fun SurfaceStatusBar() {
    val view = LocalView.current
    val darkTheme = MaterialTheme.colorScheme.isDark
    DisposableEffect(view, darkTheme) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        onDispose { controller.isAppearanceLightStatusBars = darkTheme }
    }
}
