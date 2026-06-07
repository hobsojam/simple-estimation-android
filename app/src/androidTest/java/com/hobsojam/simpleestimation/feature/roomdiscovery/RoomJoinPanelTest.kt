package com.hobsojam.simpleestimation.feature.roomdiscovery

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class RoomJoinPanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun joinButtonInvokesCallback() {
        var joinCalled = false
        composeRule.setContent {
            MaterialTheme {
                RoomJoinPanel(
                    joinState = joiningRoomState(),
                    onManualRoomInputChanged = {},
                    onDisplayNameChanged = {},
                    onAccessPinChanged = {},
                    onCancelJoin = {},
                    onSubmitJoin = { joinCalled = true },
                )
            }
        }

        composeRule.onNodeWithText("Join room").performClick()

        assert(joinCalled) { "onSubmitJoin was not called" }
    }

    @Test
    fun cancelButtonInvokesCallback() {
        var cancelCalled = false
        composeRule.setContent {
            MaterialTheme {
                RoomJoinPanel(
                    joinState = joiningRoomState(),
                    onManualRoomInputChanged = {},
                    onDisplayNameChanged = {},
                    onAccessPinChanged = {},
                    onCancelJoin = { cancelCalled = true },
                    onSubmitJoin = {},
                )
            }
        }

        composeRule.onNodeWithText("Cancel").performClick()

        assert(cancelCalled) { "onCancelJoin was not called" }
    }

    @Test
    fun errorStatusRendersErrorMessage() {
        composeRule.setContent {
            MaterialTheme {
                RoomJoinPanel(
                    joinState = joiningRoomState(status = RoomJoinStatus.Error("Room not found.")),
                    onManualRoomInputChanged = {},
                    onDisplayNameChanged = {},
                    onAccessPinChanged = {},
                    onCancelJoin = {},
                    onSubmitJoin = {},
                )
            }
        }

        composeRule.onNodeWithText("Room not found.").assertIsDisplayed()
    }

    @Test
    fun checkingCompatibilityStatusRendersCheckingText() {
        composeRule.setContent {
            MaterialTheme {
                RoomJoinPanel(
                    joinState = joiningRoomState(status = RoomJoinStatus.CheckingCompatibility),
                    onManualRoomInputChanged = {},
                    onDisplayNameChanged = {},
                    onAccessPinChanged = {},
                    onCancelJoin = {},
                    onSubmitJoin = {},
                )
            }
        }

        composeRule.onNodeWithText("Checking server compatibility…").assertIsDisplayed()
    }

    @Test
    fun readyToConnectRendersParticipantNameWithoutDemoWarning() {
        composeRule.setContent {
            MaterialTheme {
                RoomJoinPanel(
                    joinState = joiningRoomState(
                        status = RoomJoinStatus.ReadyToConnect(
                            request = sampleJoinRequest(),
                            demoMode = false,
                        ),
                    ),
                    onManualRoomInputChanged = {},
                    onDisplayNameChanged = {},
                    onAccessPinChanged = {},
                    onCancelJoin = {},
                    onSubmitJoin = {},
                )
            }
        }

        composeRule.onNodeWithText("Ready to connect as Avery.").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Demo mode is enabled on this server. Room data may reset without notice.",
            useUnmergedTree = true,
        ).assertIsNotDisplayed()
    }

    @Test
    fun readyToConnectWithDemoModeRendersDemoWarning() {
        composeRule.setContent {
            MaterialTheme {
                RoomJoinPanel(
                    joinState = joiningRoomState(
                        status = RoomJoinStatus.ReadyToConnect(
                            request = sampleJoinRequest(),
                            demoMode = true,
                        ),
                    ),
                    onManualRoomInputChanged = {},
                    onDisplayNameChanged = {},
                    onAccessPinChanged = {},
                    onCancelJoin = {},
                    onSubmitJoin = {},
                )
            }
        }

        composeRule.onNodeWithText(
            "Demo mode is enabled on this server. Room data may reset without notice.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Ready to connect as Avery.").assertIsDisplayed()
    }

    @Test
    fun accessPinFieldShownWhenJoiningRoom() {
        composeRule.setContent {
            MaterialTheme {
                RoomJoinPanel(
                    joinState = joiningRoomState(accessPinRequired = false),
                    onManualRoomInputChanged = {},
                    onDisplayNameChanged = {},
                    onAccessPinChanged = {},
                    onCancelJoin = {},
                    onSubmitJoin = {},
                )
            }
        }

        composeRule.onNodeWithText("Access PIN (if required)").assertIsDisplayed()
    }

    @Test
    fun accessPinLabelIndicatesRequiredWhenRoomRequiresPin() {
        composeRule.setContent {
            MaterialTheme {
                RoomJoinPanel(
                    joinState = joiningRoomState(accessPinRequired = true),
                    onManualRoomInputChanged = {},
                    onDisplayNameChanged = {},
                    onAccessPinChanged = {},
                    onCancelJoin = {},
                    onSubmitJoin = {},
                )
            }
        }

        composeRule.onNodeWithText("Access PIN").assertIsDisplayed()
    }

    @Test
    fun accessPinFieldHiddenInManualEntryMode() {
        composeRule.setContent {
            MaterialTheme {
                RoomJoinPanel(
                    joinState = RoomJoinUiState(mode = RoomJoinMode.ManualEntry),
                    onManualRoomInputChanged = {},
                    onDisplayNameChanged = {},
                    onAccessPinChanged = {},
                    onCancelJoin = {},
                    onSubmitJoin = {},
                )
            }
        }

        composeRule.onNodeWithText("Access PIN (if required)", useUnmergedTree = true)
            .assertIsNotDisplayed()
        composeRule.onNodeWithText("Access PIN", useUnmergedTree = true)
            .assertIsNotDisplayed()
    }
}

private fun joiningRoomState(
    status: RoomJoinStatus = RoomJoinStatus.Idle,
    accessPinRequired: Boolean = false,
) = RoomJoinUiState(
    mode = RoomJoinMode.JoiningRoom(
        roomIdInput = "room-99",
        roomName = "Sprint planning",
        accessPinRequired = accessPinRequired,
    ),
    displayName = "Avery",
    status = status,
)

private fun sampleJoinRequest() = RoomJoinRequest(
    serverBaseUrl = "https://example.com",
    roomId = "room-99",
    participantId = "participant-1",
    displayName = "Avery",
    accessPin = null,
)
