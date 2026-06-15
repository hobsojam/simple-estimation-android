package com.hobsojam.simpleestimation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Simple Estimation — typography.
 * The product uses the native system font on every platform, so Android uses
 * FontFamily.Default (the device system font) — no font files to bundle.
 * The rem scale from `tokens/typography.css` is converted to sp at a 16px root.
 *
 * Mapping of design-system roles to M3 slots:
 *   2xl 1.6rem/600  -> headlineSmall   (page wordmark / title)
 *   xl  1.5rem/600  -> titleLarge      (screen title, e.g. "Planning Poker")
 *   lg  1.1rem/600  -> titleMedium     (sub-section heading)
 *   md  1rem/400    -> bodyLarge       (body, control text)
 *   sm  0.9rem/400  -> bodyMedium      (secondary body)
 *   md/600 button   -> labelLarge      (button / action labels)
 *   eyebrow 1rem    -> labelMedium     (UPPERCASE section labels; apply
 *                                       letterSpacing + uppercase at call site)
 *   badge 0.72rem   -> labelSmall
 */
val SimpleEstimationTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp, // ~0.05em eyebrow tracking
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

/*
 * The one monospace moment: room IDs and codes. Use this style explicitly,
 * e.g. Text("Room: a1b2c3d4", style = MonospaceMeta).
 */
val MonospaceMeta = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
)
