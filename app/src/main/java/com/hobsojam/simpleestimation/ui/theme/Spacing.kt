package com.hobsojam.simpleestimation.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Simple Estimation — spacing scale, from `tokens/spacing.css`.
 * Compose has no built-in spacing system, so expose it through the theme as
 * `MaterialTheme.spacing` (see Theme.kt). Compact, mostly-4px rhythm.
 */
data class Spacing(
    val xs: Dp = 6.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 10.dp,
    val lg: Dp = 12.dp,
    val xl: Dp = 16.dp,
    val xxl: Dp = 20.dp,
    val xxxl: Dp = 24.dp,
    // Screen gutter; 16.dp on compact widths.
    val screen: Dp = 24.dp,
    // Accessibility: minimum interactive target carried from the apps.
    val tapMin: Dp = 48.dp,
)

val DefaultSpacing = Spacing()
