package com.hobsojam.simpleestimation.feature.itemplacement

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ItemPlacementScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsRoomNameAndParticipantInfo() {
        composeRule.setContent {
            MaterialTheme {
                ItemPlacementScreen(
                    state = bucketState(),
                    onItemSelected = {},
                    onMoveItem = { _, _ -> },
                    onLeave = {},
                )
            }
        }

        composeRule.onNodeWithText("Sprint planning").assertIsDisplayed()
        composeRule.onNodeWithText("Joined as Avery · 3 participants").assertIsDisplayed()
    }

    @Test
    fun showsUnplacedSectionAndBucketPositions() {
        composeRule.setContent {
            MaterialTheme {
                ItemPlacementScreen(
                    state = bucketState(),
                    onItemSelected = {},
                    onMoveItem = { _, _ -> },
                    onLeave = {},
                )
            }
        }

        composeRule.onNodeWithText("Unplaced").assertIsDisplayed()
        listOf("XS", "S", "M", "L", "XL").forEach { position ->
            composeRule.onNodeWithText(position).assertIsDisplayed()
        }
    }

    @Test
    fun showsRelativePositionsForRelativeRoom() {
        composeRule.setContent {
            MaterialTheme {
                ItemPlacementScreen(
                    state = relativeState(),
                    onItemSelected = {},
                    onMoveItem = { _, _ -> },
                    onLeave = {},
                )
            }
        }

        listOf("1", "2", "3", "5", "8", "13", "21").forEach { position ->
            composeRule.onNodeWithText(position).assertIsDisplayed()
        }
    }

    @Test
    fun tappingItemSelectsIt() {
        var selectedId: String? by mutableStateOf(null)
        composeRule.setContent {
            MaterialTheme {
                ItemPlacementScreen(
                    state = bucketState(selectedItemId = selectedId),
                    onItemSelected = { selectedId = it },
                    onMoveItem = { _, _ -> },
                    onLeave = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Feature A").performClick()

        composeRule.onNode(
            hasStateDescription("Selected"),
        ).assertIsDisplayed()
    }

    @Test
    fun tappingPlaceButtonInvokesOnMoveItem() {
        var movedItemId: String? = null
        var movedPosition: String? = "sentinel"
        composeRule.setContent {
            MaterialTheme {
                ItemPlacementScreen(
                    state = bucketState(selectedItemId = "item-1"),
                    onItemSelected = {},
                    onMoveItem = { itemId, position ->
                        movedItemId = itemId
                        movedPosition = position
                    },
                    onLeave = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Place in M").performClick()

        assert(movedItemId == "item-1")
        assert(movedPosition == "M")
    }

    @Test
    fun tappingPlaceInUnplacedMovesItemToNull() {
        var movedPosition: String? = "sentinel"
        composeRule.setContent {
            MaterialTheme {
                ItemPlacementScreen(
                    state = bucketState(selectedItemId = "item-2"),
                    onItemSelected = {},
                    onMoveItem = { _, position -> movedPosition = position },
                    onLeave = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Move to unplaced").performClick()

        assert(movedPosition == null)
    }

    @Test
    fun leaveButtonInvokesCallback() {
        var leaveInvoked = false
        composeRule.setContent {
            MaterialTheme {
                ItemPlacementScreen(
                    state = bucketState(),
                    onItemSelected = {},
                    onMoveItem = { _, _ -> },
                    onLeave = { leaveInvoked = true },
                )
            }
        }

        composeRule.onNodeWithText("Leave").performClick()

        assert(leaveInvoked)
    }

    @Test
    fun serverErrorBannerIsShownWhenPresent() {
        composeRule.setContent {
            MaterialTheme {
                ItemPlacementScreen(
                    state = bucketState(serverError = "Join the room before moving items."),
                    onItemSelected = {},
                    onMoveItem = { _, _ -> },
                    onLeave = {},
                )
            }
        }

        composeRule.onNodeWithText("Join the room before moving items.").assertIsDisplayed()
    }
}

private fun bucketState(selectedItemId: String? = null, serverError: String? = null) =
    ItemPlacementUiState(
        roomName = "Sprint planning",
        participantName = "Avery",
        participantCount = 3,
        positions = listOf("XS", "S", "M", "L", "XL"),
        unplacedItems = listOf(PlacementItem(id = "item-1", label = "Feature A")),
        placedItems = mapOf(
            "XS" to emptyList(),
            "S" to emptyList(),
            "M" to emptyList(),
            "L" to listOf(PlacementItem(id = "item-2", label = "Feature B")),
            "XL" to emptyList(),
        ),
        selectedItemId = selectedItemId,
        serverError = serverError,
    )

private fun relativeState() = ItemPlacementUiState(
    roomName = "Sprint planning",
    participantName = "Avery",
    participantCount = 2,
    positions = listOf("1", "2", "3", "5", "8", "13", "21"),
    unplacedItems = listOf(PlacementItem(id = "item-1", label = "Feature A")),
    placedItems = mapOf(
        "1" to emptyList(),
        "2" to emptyList(),
        "3" to emptyList(),
        "5" to emptyList(),
        "8" to emptyList(),
        "13" to emptyList(),
        "21" to emptyList(),
    ),
)
