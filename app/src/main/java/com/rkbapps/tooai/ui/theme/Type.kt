package com.rkbapps.tooai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rkbapps.tooai.R

private fun googleSansFlex(weight: FontWeight) = Font(
    resId = R.font.google_sans_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val GoogleSansFlex = FontFamily(
    googleSansFlex(FontWeight.Normal),
    googleSansFlex(FontWeight.Medium),
    googleSansFlex(FontWeight.SemiBold),
    googleSansFlex(FontWeight.Bold),
    googleSansFlex(FontWeight.ExtraBold),
)

private val BaseTypography = Typography(fontFamily = GoogleSansFlex)

// Baseline styles stay on the M3 scale. Emphasized styles follow the design's type scale.
val Typography = BaseTypography.copy(
    displayLargeEmphasized = BaseTypography.displayLargeEmphasized.copy(
        fontWeight = FontWeight.ExtraBold
    ),
    displayMediumEmphasized = BaseTypography.displayMediumEmphasized.copy(
        fontWeight = FontWeight.ExtraBold
    ),
    displaySmallEmphasized = BaseTypography.displaySmallEmphasized.copy(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp
    ),
    headlineLargeEmphasized = BaseTypography.headlineLargeEmphasized.copy(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMediumEmphasized = BaseTypography.headlineMediumEmphasized.copy(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmallEmphasized = BaseTypography.headlineSmallEmphasized.copy(
        fontWeight = FontWeight.ExtraBold
    ),
    titleLargeEmphasized = BaseTypography.titleLargeEmphasized.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMediumEmphasized = BaseTypography.titleMediumEmphasized.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmallEmphasized = BaseTypography.titleSmallEmphasized.copy(
        fontWeight = FontWeight.Bold
    ),
    labelLargeEmphasized = BaseTypography.labelLargeEmphasized.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMediumEmphasized = BaseTypography.labelMediumEmphasized.copy(
        fontWeight = FontWeight.SemiBold
    ),
    labelSmallEmphasized = BaseTypography.labelSmallEmphasized.copy(
        fontWeight = FontWeight.SemiBold
    ),
)
