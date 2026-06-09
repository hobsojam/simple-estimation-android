package com.hobsojam.simpleestimation.ui.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hobsojam.simpleestimation.ui.theme.semanticColors

/* Which semantic triad a Banner uses. */
enum class BannerTone { INFO, SUCCESS, WARNING, DANGER }

/*
 * Banner — a full-width inline message strip (active item, errors, demo/timer
 * notices). Demonstrates reading the custom `MaterialTheme.semanticColors`
 * extension, since success/warning/info have no Material 3 ColorScheme slot.
 */
@Composable
fun Banner(text: String, tone: BannerTone = BannerTone.INFO, modifier: Modifier = Modifier) {
    val s = MaterialTheme.semanticColors
    val (surface, border, content) = when (tone) {
        BannerTone.INFO -> Triple(s.infoSurface, s.infoBorder, s.infoContent)
        BannerTone.SUCCESS -> Triple(s.successSurface, s.successBorder, s.successContent)
        BannerTone.WARNING -> Triple(s.warningSurface, s.warningBorder, s.warningContent)
        BannerTone.DANGER -> Triple(s.dangerSurface, s.dangerBorder, s.dangerContent)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium, // 6dp
        color = surface,
        border = BorderStroke(1.dp, border),
    ) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
