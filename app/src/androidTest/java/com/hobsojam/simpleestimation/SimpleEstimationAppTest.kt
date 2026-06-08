package com.hobsojam.simpleestimation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryStatus
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryUiState
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinMode
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinRequest
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinStatus
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinUiState
import com.hobsojam.simpleestimation.feature.roomsession.RoomSessionState
import org.junit.Rule
import org.junit.Test

class SimpleEstimationAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    // Verifies that a ReadyToConnect join status keeps the discovery screen visible rather than
    // navigating away. State is set directly because the join panel sits below the viewport fold
    // on the emulator, making performClick() on its button unreliable in a non-scrolling column.
    // Button click behaviour is covered by RoomJoinPanelTest.joinButtonInvokesCallback.
    @Test
    fun discoveryScreenRemainsVisibleWhenJoinStatusIsReadyToConnect() {
        composeRule.setContent {
            SimpleEstimationApp(
                uiState = RoomDiscoveryUiState(
                    serverUrl = "https://example.com",
                    status = RoomDiscoveryStatus.Idle,
                    join = RoomJoinUiState(
                        mode = RoomJoinMode.JoiningRoom(
                            roomIdInput = "room-99",
                            roomName = null,
                            accessPinRequired = false,
                        ),
                        displayName = "Avery",
                        status = RoomJoinStatus.ReadyToConnect(
                            request = RoomJoinRequest(
                                serverBaseUrl = "https://example.com",
                                roomId = "room-99",
                                participantId = "participant-1",
                                displayName = "Avery",
                                accessPin = null,
                            ),
                            demoMode = true,
                        ),
                    ),
                ),
                onServerUrlChanged = {},
                onLoadRooms = {},
                onManualRoomInputChanged = {},
                onRoomSelected = {},
                onDisplayNameChanged = {},
                onAccessPinChanged = {},
                onCancelJoin = {},
                onSubmitJoin = {},
                sessionState = RoomSessionState.Idle,
                displayName = "Avery",
                onSessionConnect = {},
                onVote = { false },
                onMoveItem = { _, _ -> false },
                onLeaveSession = {},
            )
        }

        composeRule.onNodeWithText("Active rooms").assertIsDisplayed()
        // Status messages may be below the viewport fold; assertExists confirms they are in the
        // composition without requiring the column to be scrollable.
        composeRule.onNodeWithText(
            "Demo mode is enabled on this server. Room data may reset without notice.",
        ).assertExists()
        composeRule.onNodeWithText("Ready to connect as Avery.").assertExists()
    }
}
