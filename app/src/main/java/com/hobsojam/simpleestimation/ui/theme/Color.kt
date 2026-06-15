package com.hobsojam.simpleestimation.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Simple Estimation — color palette.
 * Values are lifted verbatim from the design system's `tokens/colors.css`.
 * Keep this file as the single source of brand color truth on Android.
 */

// ---- Brand (blue) ----
val Blue600 = Color(0xFF2563EB) // primary action
val Blue700 = Color(0xFF1D4ED8) // hover / pressed
val Blue800 = Color(0xFF1E40AF) // info text
val Blue50 = Color(0xFFEFF6FF) // primary tint surface
val Blue100 = Color(0xFFDBEAFE) // badge fill
val Blue200 = Color(0xFFBFDBFE) // info border

// ---- Neutrals (slate / gray) ----
val AppBackground = Color(0xFFF8FAFC) // slate-50
val White = Color(0xFFFFFFFF)
val Gray50 = Color(0xFFF9FAFB) // muted panel surface
val Gray100 = Color(0xFFF3F4F6) // sunken / board column
val Slate100 = Color(0xFFF1F5F9) // mono chip / divider
val Gray200 = Color(0xFFE5E7EB) // default border
val Gray300 = Color(0xFFD1D5DB) // control border
val Gray400 = Color(0xFF9CA3AF) // disabled glyph
val Gray500 = Color(0xFF6B7280) // placeholder
val Slate500 = Color(0xFF64748B) // subtle
val Gray600 = Color(0xFF4B5563) // muted text
val Gray700 = Color(0xFF374151) // body on light controls
val Slate800 = Color(0xFF1E293B) // primary text

// ---- Success (green) ----
val Green50 = Color(0xFFF0FDF4)
val Green300 = Color(0xFF86EFAC)
val Green700 = Color(0xFF15803D)

// ---- Danger (red) ----
val Red50 = Color(0xFFFFF5F5)
val Red100 = Color(0xFFFEE2E2)
val Red300 = Color(0xFFFCA5A5)
val Red700 = Color(0xFFB91C1C)

// ---- Warning (amber) ----
val Amber50 = Color(0xFFFFFBEB)
val Amber100 = Color(0xFFFEF3C7)
val Amber200 = Color(0xFFFDE68A)
val Amber500 = Color(0xFFF59E0B)
val Amber700 = Color(0xFF92400E)

/*
 * ---- Dark-theme tones ----
 * The source product is light-only; this dark ramp is a deliberate, on-brand
 * extension. Surfaces are slate (cool, not pure black); the brand and all
 * semantic accents are lightened so they keep contrast on dark surfaces.
 */
val DarkBackground = Color(0xFF0F172A) // slate-900 — app background
val DarkSurface = Color(0xFF1E293B) // slate-800 — cards
val DarkSurfaceVariant = Color(0xFF273449) // between slate-800/700 — muted panel
val DarkOutline = Color(0xFF475569) // slate-600 — control border
val DarkOutlineVariant = Color(0xFF334155) // slate-700 — hairline border
val DarkOnSurface = Color(0xFFE2E8F0) // slate-200 — primary text
val DarkOnSurfaceVariant = Color(0xFF94A3B8) // slate-400 — muted text

val Blue300 = Color(0xFF93C5FD) // info content on dark
val Blue400 = Color(0xFF60A5FA) // primary on dark
val Blue500 = Color(0xFF3B82F6) // pressed on dark
val Blue900 = Color(0xFF1E3A8A) // primary container on dark
val Blue950 = Color(0xFF172554) // info surface on dark

val Green300d = Color(0xFF86EFAC) // success content on dark
val Green800 = Color(0xFF166534) // success border on dark
val Green950 = Color(0xFF052E16) // success surface on dark

val Amber300 = Color(0xFFFCD34D) // warning content on dark
val Amber800 = Color(0xFF9A3412) // warning border on dark
val Amber950 = Color(0xFF422006) // warning surface on dark

val Red200 = Color(0xFFFECACA) // on error container (dark)
val Red400 = Color(0xFFF87171) // error / danger content on dark
val Red800 = Color(0xFF991B1B) // danger border on dark
val Red900 = Color(0xFF7F1D1D) // error container on dark
val Red950 = Color(0xFF450A0A) // danger surface on dark

/*
 * Material 3 ColorScheme.
 * The product is a light, flat, utilitarian UI. A dark scheme is provided below
 * as a deliberate, on-brand extension (the source product is light-only) — opt
 * in via `SimpleEstimationTheme(darkTheme = …)`, which defaults to the system.
 */
val SimpleEstimationLightColors = lightColorScheme(
    primary = Blue600,
    onPrimary = White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue800,

    secondary = Gray700,
    onSecondary = White,
    secondaryContainer = Gray100,
    onSecondaryContainer = Slate800,

    background = AppBackground,
    onBackground = Slate800,

    surface = White,
    onSurface = Slate800,
    surfaceVariant = Gray50,
    onSurfaceVariant = Gray600,

    outline = Gray300,
    outlineVariant = Gray200,

    error = Red700,
    onError = White,
    errorContainer = Red100,
    onErrorContainer = Red700,
)

/*
 * Dark ColorScheme — on-brand extension (no equivalent in the source product).
 * Brand and error are lightened for contrast against slate surfaces.
 */
val SimpleEstimationDarkColors = darkColorScheme(
    primary = Blue400,
    onPrimary = DarkBackground,
    primaryContainer = Blue900,
    onPrimaryContainer = Blue100,

    secondary = DarkOnSurfaceVariant,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,

    background = DarkBackground,
    onBackground = DarkOnSurface,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,

    error = Red400,
    onError = DarkBackground,
    errorContainer = Red900,
    onErrorContainer = Red200,
)

/*
 * Material 3 has no slots for success / warning / info — only `error`.
 * Those triads (surface + border + content) live here and are exposed through
 * a CompositionLocal in Theme.kt as `MaterialTheme.semanticColors`.
 */
data class SemanticColors(
    val successSurface: Color,
    val successBorder: Color,
    val successContent: Color,
    val warningSurface: Color,
    val warningBorder: Color,
    val warningContent: Color,
    val infoSurface: Color,
    val infoBorder: Color,
    val infoContent: Color,
    val dangerSurface: Color,
    val dangerBorder: Color,
    val dangerContent: Color,
    val brandPressed: Color,
)

val LightSemanticColors = SemanticColors(
    successSurface = Green50,
    successBorder = Green300,
    successContent = Green700,
    warningSurface = Amber100,
    warningBorder = Amber200,
    warningContent = Amber700,
    infoSurface = Blue50,
    infoBorder = Blue200,
    infoContent = Blue800,
    dangerSurface = Red100,
    dangerBorder = Red300,
    dangerContent = Red700,
    brandPressed = Blue700,
)

val DarkSemanticColors = SemanticColors(
    successSurface = Green950,
    successBorder = Green800,
    successContent = Green300d,
    warningSurface = Amber950,
    warningBorder = Amber800,
    warningContent = Amber300,
    infoSurface = Blue950,
    infoBorder = Blue900,
    infoContent = Blue300,
    dangerSurface = Red950,
    dangerBorder = Red800,
    dangerContent = Red400,
    brandPressed = Blue500,
)
