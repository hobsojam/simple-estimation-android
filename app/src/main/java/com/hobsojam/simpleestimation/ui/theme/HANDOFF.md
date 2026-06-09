# Handoff — wire the Simple Estimation theme into the Android app

Instructions for a coding agent (e.g. Claude Code) working in the
`hobsojam/simple-estimation-android` repository. Goal: replace the Material 3
default look with the branded theme in this folder, with zero behavior changes.

## Context
- The app is a participant-only Jetpack Compose client (`com.hobsojam.simpleestimation`).
- It currently has **no `ui/theme` package** — composables read raw
  `MaterialTheme` defaults. We are adding the theme and wrapping the root.
- Durable rules live in `AGENTS.md` (git workflow, validation, accessibility,
  style). Follow them. Do not change participant scope, networking, or protocol.

## Steps

1. **Branch.** From an up-to-date `main`:
   ```bash
   git fetch origin && git switch -c feat/brand-theme origin/main
   ```

2. **Add the theme package.** Copy the files from this design system's
   `android-theme/` folder into:
   ```
   app/src/main/java/com/hobsojam/simpleestimation/ui/theme/
   ├── Color.kt   Type.kt   Shape.kt   Spacing.kt   Theme.kt
   └── components/AppButtons.kt  Banner.kt  EstimationCard.kt
   ```
   The package declarations already match (`…ui.theme` / `…ui.theme.components`).

3. **Wrap the root.** In `app/src/main/java/com/hobsojam/simpleestimation/MainActivity.kt`,
   wrap the top-level `setContent { … }` body in `SimpleEstimationTheme`:
   ```kotlin
   import com.hobsojam.simpleestimation.ui.theme.SimpleEstimationTheme
   // …
   setContent {
       SimpleEstimationTheme {
           // existing root composable unchanged
       }
   }
   ```
   If a `MaterialTheme { … }` wrapper already exists there, replace it with
   `SimpleEstimationTheme { … }`.

4. **Adopt brand reads incrementally (optional in this PR).** Where screens use
   ad-hoc colors, prefer theme reads:
   - actions → `PrimaryButton` / `SecondaryButton` / `DangerButton`
   - poker card → `EstimationCard(value, selected, enabled, onSelect)`
   - status strips → `Banner(text, tone)`
   - success/warning/info → `MaterialTheme.semanticColors.*`
   Keep this PR scoped — a sweeping screen refactor can follow separately.

5. **Validate** (per `AGENTS.md`, on Windows use `.\gradlew.bat`):
   ```bash
   ./gradlew ktlintCheck detekt test lint assembleDebug
   ```
   Fix formatting by reformatting, not by suppressing rules. Detekt enforces a
   120-char line limit.

6. **Accessibility check.** Verify a touched flow with TalkBack or keyboard nav:
   visible focus order intact, 48dp targets, font scaling without clipping,
   state not communicated by color alone.

7. **PR.** Open against `main`:
   ```bash
   git push -u origin HEAD
   gh pr create --base main --title "Add Simple Estimation brand theme" \
     --body "Introduces ui/theme (M3 ColorScheme, typography, shapes, spacing, \
   semantic colors) generated from the design system, and wraps the app root in \
   SimpleEstimationTheme. No behavior or protocol changes."
   ```
   Mention the expected version bump (minor — new branded UI) in the PR body.

## Guardrails
- No new production dependencies — this is pure Compose + Material 3.
- Dark theme follows the system setting via `SimpleEstimationTheme`; verify both
  light and dark render correctly (e.g. preview with `uiMode` or toggle the OS).
- Don't persist anything, change networking, or touch the protocol.
- Don't commit generated build output or signing material.
