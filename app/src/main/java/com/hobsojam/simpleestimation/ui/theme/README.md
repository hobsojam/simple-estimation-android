# Android Compose theme — Simple Estimation

A ready-to-drop Jetpack Compose / Material 3 theme generated from this design
system's tokens. It gives the participant-only Android client (which currently
uses Material 3 defaults) the real Simple Estimation brand.

> These are **reference Kotlin files**. They are not consumed by the web design
> system; copy them into the Android repo.

## Install

1. Copy every file in this folder into the Android app, preserving the
   `components/` subfolder, at:

   ```
   app/src/main/java/com/hobsojam/simpleestimation/ui/theme/
   ├── Color.kt
   ├── Type.kt
   ├── Shape.kt
   ├── Spacing.kt
   ├── Theme.kt
   └── components/
       ├── AppButtons.kt
       ├── Banner.kt
       └── EstimationCard.kt
   ```

   The files already declare `package com.hobsojam.simpleestimation.ui.theme`
   (and `.components`), matching the app's namespace.

2. Wrap your root content in `SimpleEstimationTheme` (usually in
   `MainActivity.setContent { ... }`):

   ```kotlin
   setContent {
       SimpleEstimationTheme {            // follows the system light/dark setting
           // existing app content
       }
   }
   ```

   Force a mode with `SimpleEstimationTheme(darkTheme = true) { … }` if needed.

3. Run the repo's checks before committing (per `AGENTS.md`):
   `./gradlew ktlintCheck detekt test lint assembleDebug`.

## What you get

| Surface | How to read it |
|---|---|
| Brand + neutrals + error | `MaterialTheme.colorScheme.*` (e.g. `primary` = `#2563EB`) |
| Success / warning / info / danger triads | `MaterialTheme.semanticColors.*` (custom extension) |
| Type scale (system font) | `MaterialTheme.typography.*`; `MonospaceMeta` for room IDs |
| Corner radii (3/4/6/8/10dp) | `MaterialTheme.shapes.*` |
| Spacing scale | `MaterialTheme.spacing.*` |
| Card face | `EstimationCard(value, selected, onSelect)` |
| Buttons | `PrimaryButton` · `SecondaryButton` · `DangerButton` |
| Message strips | `Banner(text, tone)` |

## Token → Compose mapping

| Design system token | Compose |
|---|---|
| `--brand` `#2563eb` / pressed `#1d4ed8` | `colorScheme.primary` / `semanticColors.brandPressed` |
| `--surface-card` / `--bg` | `colorScheme.surface` / `background` |
| `--text-primary` `#1e293b` | `colorScheme.onSurface` |
| `--text-muted` `#4b5563` | `colorScheme.onSurfaceVariant` |
| `--border-default` `#e5e7eb` | `colorScheme.outlineVariant` |
| danger red family | `colorScheme.error` / `errorContainer` |
| success / warning / info triads | `semanticColors` (no native M3 slot) |
| radii 4 / 6 / 8 px | `shapes.small` / `medium` / `large` |
| system font stack | `FontFamily.Default` (no font files) |

## Notes & deliberate choices

- **Dark theme included.** `SimpleEstimationTheme` follows the system setting
  by default (`isSystemInDarkTheme()`); pass `darkTheme = …` to override. The
  dark ramp is an on-brand extension (the source product is light-only): cool
  slate surfaces, with the brand and all semantic accents lightened for
  contrast. Both `colorScheme` and `semanticColors` switch together.
- **Material 3 only has `error`.** Success, warning, and info are provided
  through the `SemanticColors` CompositionLocal in `Theme.kt`. Always read them
  via `MaterialTheme.semanticColors`, never hard-code the hex.
- **Accessibility floor.** Button wrappers use `heightIn(min = 48.dp)`, matching
  the `AGENTS.md` touch-target requirement. Keep select-and-place as the primary
  item-move interaction; don't rely on color alone for state (pair with text /
  content descriptions), per the repo's accessibility rules.
- **EstimationCard** is hand-written rather than a stock Material component — the
  card face is the product's one custom-styled, game-like element.
- The web design system in this project remains the source of truth. When a
  token changes there, update `Color.kt` / `Type.kt` / `Shape.kt` to match.
