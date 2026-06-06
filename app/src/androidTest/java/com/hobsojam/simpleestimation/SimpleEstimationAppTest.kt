package com.hobsojam.simpleestimation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryStatus
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryUiState
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinMode
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinRequest
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinStatus
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinUiState
import org.junit.Rule
import org.junit.Test

class SimpleEstimationAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun validJoinRequestStaysInDiscoveryUntilCompatibilityAndConnectionRun() {
        var uiState by mutableStateOf(
            RoomDiscoveryUiState(
                serverUrl = "https://example.com",
                status = RoomDiscoveryStatus.Idle,
                join = RoomJoinUiState(
                    mode = RoomJoinMode.JoiningRoom(
                        roomIdInput = "room-99",
                        roomName = null,
                        accessPinRequired = false,
                    ),
                    displayName = "Avery",
                ),
            ),
        )

        composeRule.setContent {
            SimpleEstimationApp(
                uiState = uiState,
                onServerUrlChanged = {},
                onLoadRooms = {},
                onManualRoomInputChanged = {},
                onRoomSelected = {},
                onDisplayNameChanged = {},
                onAccessPinChanged = {},
                onCancelJoin = {},
                onSubmitJoin = {
                    uiState = uiState.copy(
                        join = uiState.join.copy(
                            status = RoomJoinStatus.ReadyToConnect(
                                RoomJoinRequest(
                                    serverBaseUrl = "https://example.com",
                                    roomId = "room-99",
                                    displayName = "Avery",
                                    accessPin = null,
                                ),
                            ),
                        ),
                    )
                },
            )
        }

        composeRule.onNodeWithText("Join room").performClick()

        composeRule.onNodeWithText("Active rooms").assertIsDisplayed()
        composeRule.onNodeWithText("Ready to connect as Avery.").assertIsDisplayed()
    }
}
