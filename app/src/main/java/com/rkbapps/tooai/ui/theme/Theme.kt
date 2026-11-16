package com.rkbapps.tooai.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
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
    accentColor: AccentColor = AccentColor.INDIGO,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme.withAccent(accentColor, darkTheme = true)
        else -> LightColorScheme.withAccent(accentColor, darkTheme = false)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            updateStatusBarIcons(view, darkTheme)
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

// Page background for screens built from contained rows.
@Composable
fun pageContainerColor() = if (MaterialTheme.colorScheme.isDark) {
    MaterialTheme.colorScheme.surface
} else {
    MaterialTheme.colorScheme.surfaceContainerLow
}

// One step above pageContainerColor. surfaceContainerLowest is darker than the page in dark mode.
@Composable
fun rowContainerColor() = if (MaterialTheme.colorScheme.isDark) {
    MaterialTheme.colorScheme.surfaceContainer
} else {
    MaterialTheme.colorScheme.surfaceContainerLowest
}

// Screens stay composed through a transition, so two can overlap. Counted to avoid one
// screen's exit resetting the icons of the one replacing it.
private var surfaceStatusBarScreens = 0

// Icons contrast with the primary-colored top bars unless a SurfaceStatusBar screen is showing.
private fun updateStatusBarIcons(view: View, darkTheme: Boolean) {
    val window = (view.context as Activity).window
    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
        if (surfaceStatusBarScreens > 0) !darkTheme else darkTheme
}

// For screens without a primary-colored top bar.
@Composable
fun SurfaceStatusBar() {
    val view = LocalView.current
    val darkTheme = MaterialTheme.colorScheme.isDark
    DisposableEffect(view, darkTheme) {
        surfaceStatusBarScreens++
        updateStatusBarIcons(view, darkTheme)
        onDispose {
            surfaceStatusBarScreens--
            updateStatusBarIcons(view, darkTheme)
        }
    }
}
