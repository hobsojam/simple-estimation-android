package com.hobsojam.simpleestimation.feature.itemplacement

import com.hobsojam.simpleestimation.domain.room.PositionedItem
import com.hobsojam.simpleestimation.domain.room.SessionRoomState

internal val BUCKET_POSITIONS = listOf("XS", "S", "M", "L", "XL")
internal val RELATIVE_POSITIONS = listOf("1", "2", "3", "5", "8", "13", "21")

internal fun SessionRoomState.Bucket.toUiState(
    displayName: String,
    selectedItemId: String? = null,
    serverError: String? = null,
): ItemPlacementUiState = mapToUiState(
    name = name,
    displayName = displayName,
    participantCount = participants.size,
    positions = BUCKET_POSITIONS,
    items = items,
    selectedItemId = selectedItemId,
    serverError = serverError,
)

internal fun SessionRoomState.Relative.toUiState(
    displayName: String,
    selectedItemId: String? = null,
    serverError: String? = null,
): ItemPlacementUiState = mapToUiState(
    name = name,
    displayName = displayName,
    participantCount = participants.size,
    positions = RELATIVE_POSITIONS,
    items = items,
    selectedItemId = selectedItemId,
    serverError = serverError,
)

private fun mapToUiState(
    name: String?,
    displayName: String,
    participantCount: Int,
    positions: List<String>,
    items: List<PositionedItem>,
    selectedItemId: String?,
    serverError: String?,
): ItemPlacementUiState {
    val grouped = items.groupBy { it.position }
    return ItemPlacementUiState(
        roomName = name ?: "",
        participantName = displayName,
        participantCount = participantCount,
        positions = positions,
        unplacedItems = (grouped[null] ?: emptyList()).map { PlacementItem(it.id, it.label) },
        placedItems = positions.associateWith { pos ->
            (grouped[pos] ?: emptyList()).map { PlacementItem(it.id, it.label) }
        },
        selectedItemId = selectedItemId,
        serverError = serverError,
    )
}
