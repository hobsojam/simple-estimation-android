package com.hobsojam.simpleestimation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/*
 * Simple Estimation — theme entry point.
 *
 * Wrap your app content in `SimpleEstimationTheme { ... }` (typically in
 * MainActivity's setContent). Standard M3 reads resolve to the brand:
 *   MaterialTheme.colorScheme.primary   -> Blue600
 *   MaterialTheme.typography.titleLarge -> screen title style
 *   MaterialTheme.shapes.medium         -> 6dp card radius
 *
 * The two things M3 can't hold are exposed as extensions:
 *   MaterialTheme.semanticColors -> success / warning / info / danger triads
 *   MaterialTheme.spacing        -> the spacing scale
 */

private val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }
private val LocalSpacing = staticCompositionLocalOf { DefaultSpacing }

val MaterialTheme.semanticColors: SemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSemanticColors.current

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

@Composable
fun SimpleEstimationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SimpleEstimationDarkColors else SimpleEstimationLightColors
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(
        LocalSemanticColors provides semanticColors,
        LocalSpacing provides DefaultSpacing,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SimpleEstimationTypography,
            shapes = SimpleEstimationShapes,
            content = content,
        )
    }
}
