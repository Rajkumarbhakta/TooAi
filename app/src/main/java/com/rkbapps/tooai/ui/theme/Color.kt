package com.rkbapps.tooai.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Seed palette, used when dynamic color is unavailable.
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5847C8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF2A1A8A),
    inversePrimary = Color(0xFFC9BFFF),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6DEF8),
    onSecondaryContainer = Color(0xFF1E182D),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBD),
    onTertiaryContainer = Color(0xFF4A2800),
    background = Color(0xFFFDF8FF),
    onBackground = Color(0xFF1C1A22),
    surface = Color(0xFFFDF8FF),
    onSurface = Color(0xFF1C1A22),
    surfaceVariant = Color(0xFFE6E0EA),
    onSurfaceVariant = Color(0xFF48454F),
    surfaceTint = Color(0xFF5847C8),
    inverseSurface = Color(0xFF322F37),
    inverseOnSurface = Color(0xFFF5EFF7),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF79757F),
    outlineVariant = Color(0xFFCAC4D0),
    surfaceBright = Color(0xFFFDF8FF),
    surfaceDim = Color(0xFFDED8E1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FB),
    surfaceContainer = Color(0xFFF1ECF6),
    surfaceContainerHigh = Color(0xFFEBE6F0),
    surfaceContainerHighest = Color(0xFFE6E0EA),
    primaryFixed = Color(0xFFE5DEFF),
    primaryFixedDim = Color(0xFFC9BFFF),
    onPrimaryFixed = Color(0xFF2A1A8A),
    onPrimaryFixedVariant = Color(0xFF4636B0),
    secondaryFixed = Color(0xFFE6DEF8),
    secondaryFixedDim = Color(0xFFCCC2DC),
    onSecondaryFixed = Color(0xFF1E182D),
    onSecondaryFixedVariant = Color(0xFF4A4458),
    tertiaryFixed = Color(0xFFFFDCBD),
    tertiaryFixedDim = Color(0xFFFFB870),
    onTertiaryFixed = Color(0xFF4A2800),
    onTertiaryFixedVariant = Color(0xFF6B3E00),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC9BFFF),
    onPrimary = Color(0xFF2B1A86),
    primaryContainer = Color(0xFF4636B0),
    onPrimaryContainer = Color(0xFFE5DEFF),
    inversePrimary = Color(0xFF5847C8),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFFFB870),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6B3E00),
    onTertiaryContainer = Color(0xFFFFDCBD),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = Color(0xFFC9BFFF),
    inverseSurface = Color(0xFFE6E0E9),
    inverseOnSurface = Color(0xFF322F37),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    surfaceBright = Color(0xFF3B383E),
    surfaceDim = Color(0xFF141218),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    primaryFixed = Color(0xFFE5DEFF),
    primaryFixedDim = Color(0xFFC9BFFF),
    onPrimaryFixed = Color(0xFF2A1A8A),
    onPrimaryFixedVariant = Color(0xFF4636B0),
    secondaryFixed = Color(0xFFE6DEF8),
    secondaryFixedDim = Color(0xFFCCC2DC),
    onSecondaryFixed = Color(0xFF1E182D),
    onSecondaryFixedVariant = Color(0xFF4A4458),
    tertiaryFixed = Color(0xFFFFDCBD),
    tertiaryFixedDim = Color(0xFFFFB870),
    onTertiaryFixed = Color(0xFF4A2800),
    onTertiaryFixedVariant = Color(0xFF6B3E00),
)

// Each tool keeps its identity, so these stay fixed even when dynamic color is on.
@Immutable
data class ToolColors(
    val container: Color,
    val onContainer: Color,
    val action: Color,
    val onAction: Color,
)

@Immutable
data class ToolPalette(
    val aiChat: ToolColors,
    val docs: ToolColors,
    val qr: ToolColors,
    val background: ToolColors,
    val text: ToolColors,
)

val LightToolPalette = ToolPalette(
    aiChat = ToolColors(
        container = Color(0xFFE5DEFF),
        onContainer = Color(0xFF2A1A8A),
        action = Color(0xFF5847C8),
        onAction = Color(0xFFFFFFFF),
    ),
    docs = ToolColors(
        container = Color(0xFFFFDCBD),
        onContainer = Color(0xFF4A2800),
        action = Color(0xFF8A5100),
        onAction = Color(0xFFFFFFFF),
    ),
    qr = ToolColors(
        container = Color(0xFFC4EED0),
        onContainer = Color(0xFF00391A),
        action = Color(0xFF1D6B3C),
        onAction = Color(0xFFFFFFFF),
    ),
    background = ToolColors(
        container = Color(0xFFFFD8E8),
        onContainer = Color(0xFF5B0A33),
        action = Color(0xFF8E2C5C),
        onAction = Color(0xFFFFFFFF),
    ),
    text = ToolColors(
        container = Color(0xFFD1E4FF),
        onContainer = Color(0xFF003258),
        action = Color(0xFF2F6DB5),
        onAction = Color(0xFFFFFFFF),
    ),
)

val DarkToolPalette = ToolPalette(
    aiChat = ToolColors(
        container = Color(0xFF4636B0),
        onContainer = Color(0xFFE5DEFF),
        action = Color(0xFFC9BFFF),
        onAction = Color(0xFF2B1A86),
    ),
    docs = ToolColors(
        container = Color(0xFF6B3E00),
        onContainer = Color(0xFFFFDCBD),
        action = Color(0xFFFFB870),
        onAction = Color(0xFF4A2800),
    ),
    qr = ToolColors(
        container = Color(0xFF005229),
        onContainer = Color(0xFFC4EED0),
        action = Color(0xFF8ED6A4),
        onAction = Color(0xFF00391A),
    ),
    background = ToolColors(
        container = Color(0xFF7B2950),
        onContainer = Color(0xFFFFD8E8),
        action = Color(0xFFFFB0D0),
        onAction = Color(0xFF5B0A33),
    ),
    text = ToolColors(
        container = Color(0xFF004880),
        onContainer = Color(0xFFD1E4FF),
        action = Color(0xFFA0CAFD),
        onAction = Color(0xFF003258),
    ),
)

val LocalToolPalette = staticCompositionLocalOf { LightToolPalette }
