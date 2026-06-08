package com.hobsojam.simpleestimation.feature.itemplacement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ItemPlacementScreen(
    state: ItemPlacementUiState,
    onItemSelected: (String?) -> Unit,
    onMoveItem: (itemId: String, position: String?) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlacementHeader(state = state, onLeave = onLeave)
        state.serverError?.let { PlacementErrorBanner(message = it) }
        PlacementSection(
            label = "Unplaced",
            items = state.unplacedItems,
            selectedItemId = state.selectedItemId,
            onPlaceClick = state.selectedItemId?.let { id -> { onMoveItem(id, null) } },
            placeContentDescription = "Move to unplaced",
            onItemClick = { item ->
                onItemSelected(if (item.id == state.selectedItemId) null else item.id)
            },
        )
        state.positions.forEach { position ->
            PlacementSection(
                label = position,
                items = state.placedItems[position] ?: emptyList(),
                selectedItemId = state.selectedItemId,
                onPlaceClick = state.selectedItemId?.let { id -> { onMoveItem(id, position) } },
                placeContentDescription = "Place in $position",
                onItemClick = { item ->
                    onItemSelected(if (item.id == state.selectedItemId) null else item.id)
                },
            )
        }
    }
}

@Composable
private fun PlacementHeader(state: ItemPlacementUiState, onLeave: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.roomName,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            val count = state.participantCount
            val suffix = if (count == 1) "participant" else "participants"
            Text(
                text = "Joined as ${state.participantName} · $count $suffix",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(onClick = onLeave) { Text("Leave") }
    }
}

@Composable
private fun PlacementErrorBanner(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun PlacementSection(
    label: String,
    items: List<PlacementItem>,
    selectedItemId: String?,
    onPlaceClick: (() -> Unit)?,
    placeContentDescription: String,
    onItemClick: (PlacementItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (onPlaceClick != null) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
                if (onPlaceClick != null) {
                    TextButton(
                        onClick = onPlaceClick,
                        modifier = Modifier.semantics {
                            contentDescription = placeContentDescription
                        },
                    ) {
                        Text("Place")
                    }
                }
            }
            PlacementItems(
                items = items,
                selectedItemId = selectedItemId,
                onItemClick = onItemClick,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlacementItems(
    items: List<PlacementItem>,
    selectedItemId: String?,
    onItemClick: (PlacementItem) -> Unit,
) {
    if (items.isEmpty()) return
    FlowRow(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            val isSelected = item.id == selectedItemId
            FilterChip(
                selected = isSelected,
                onClick = { onItemClick(item) },
                label = { Text(item.label) },
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = item.label
                    stateDescription = if (isSelected) "Selected" else "Not selected"
                },
            )
        }
    }
}
