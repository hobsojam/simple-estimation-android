package com.hobsojam.simpleestimation.ui.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hobsojam.simpleestimation.ui.theme.CardValueStyle

/*
 * EstimationCard — the planning-poker card face, the product's one tactile,
 * game-like element. A 54x72 tile with a 2dp border that fills brand-blue when
 * selected. Faithful port of the web `EstimationCard` component.
 *
 * Note: the web design is 54x72 px; on Android the values are kept as dp but
 * the card is allowed to grow for font scaling. For a denser deck, wrap these
 * in a FlowRow with 8.dp spacing.
 */
@Composable
fun EstimationCard(
    value: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val border = if (selected) colors.primary else colors.outline
    val background = if (selected) colors.primary else colors.surface
    val content = if (selected) colors.onPrimary else colors.onSurface

    Surface(
        modifier = modifier.size(width = 54.dp, height = 72.dp),
        shape = MaterialTheme.shapes.medium,
        color = background,
        border = BorderStroke(2.dp, border),
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = { onSelect(value) },
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                color = content,
                style = CardValueStyle,
                textAlign = TextAlign.Center,
            )
        }
    }
}
