package com.hobsojam.simpleestimation.feature.itemplacement

data class ItemPlacementUiState(
    val roomName: String,
    val participantName: String,
    val participantCount: Int,
    val positions: List<String>,
    val unplacedItems: List<PlacementItem>,
    val placedItems: Map<String, List<PlacementItem>>,
    val selectedItemId: String? = null,
    val serverError: String? = null,
)

data class PlacementItem(val id: String, val label: String)
