package com.example.sequencelab.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

/** Tabular figures keep digits aligned in sequences. */
private const val TABULAR = "tnum"

val Typography = Typography(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR),
    bodyLarge = base.bodyLarge.copy(fontFeatureSettings = TABULAR),
    bodyMedium = base.bodyMedium.copy(fontFeatureSettings = TABULAR),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp),
)

/** Serif style gives formulas a textbook feel. */
val FormulaStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 17.sp,
    lineHeight = 24.sp,
    fontFeatureSettings = TABULAR,
)
