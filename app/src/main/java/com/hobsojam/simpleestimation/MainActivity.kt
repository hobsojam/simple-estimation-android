package com.hobsojam.simpleestimation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hobsojam.simpleestimation.data.room.HttpActiveRoomRepository
import com.hobsojam.simpleestimation.data.server.JavaNetServerConfigClient
import com.hobsojam.simpleestimation.domain.room.ActiveRoom
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryScreen
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryStatus
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryUiState
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryViewModel
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinStatus
import com.hobsojam.simpleestimation.feature.roomsession.RoomSessionScreen
import com.hobsojam.simpleestimation.feature.roomsession.RoomSessionState
import com.hobsojam.simpleestimation.feature.roomsession.RoomSessionViewModel

class MainActivity : ComponentActivity() {
    private val roomDiscoveryViewModel: RoomDiscoveryViewModel by viewModels {
        RoomDiscoveryViewModel.Factory(
            repositoryFactory = { HttpActiveRoomRepository() },
            configClientFactory = { JavaNetServerConfigClient() },
        )
    }

    private val roomSessionViewModel: RoomSessionViewModel by viewModels {
        RoomSessionViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openRoomLinkFromIntent(intent)

        setContent {
            SimpleEstimationApp(
                uiState = roomDiscoveryViewModel.uiState,
                sessionState = roomSessionViewModel.sessionState,
                displayName = roomSessionViewModel.displayName,
                onServerUrlChanged = roomDiscoveryViewModel::updateServerUrl,
                onLoadRooms = roomDiscoveryViewModel::loadActiveRooms,
                onManualRoomInputChanged = roomDiscoveryViewModel::updateManualRoomInput,
                onRoomSelected = roomDiscoveryViewModel::selectRoom,
                onDisplayNameChanged = roomDiscoveryViewModel::updateDisplayName,
                onAccessPinChanged = roomDiscoveryViewModel::updateAccessPin,
                onCancelJoin = roomDiscoveryViewModel::cancelJoin,
                onSubmitJoin = roomDiscoveryViewModel::submitJoin,
                onSessionConnect = roomSessionViewModel::connect,
                onVote = roomSessionViewModel::sendVote,
                onLeaveSession = roomSessionViewModel::disconnect,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRoomLinkFromIntent(intent)
    }

    private fun openRoomLinkFromIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString?.let(roomDiscoveryViewModel::openRoomLink)
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
                ?.let(roomDiscoveryViewModel::openRoomLink)
        }
    }
}

@Composable
fun SimpleEstimationApp(
    uiState: RoomDiscoveryUiState,
    sessionState: RoomSessionState,
    displayName: String?,
    onServerUrlChanged: (String) -> Unit,
    onLoadRooms: () -> Unit,
    onManualRoomInputChanged: (String) -> Unit,
    onRoomSelected: (ActiveRoom) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onAccessPinChanged: (String) -> Unit,
    onCancelJoin: () -> Unit,
    onSubmitJoin: () -> Unit,
    onSessionConnect: (com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinRequest) -> Unit,
    onVote: (String) -> Boolean,
    onLeaveSession: () -> Unit,
) {
    LaunchedEffect(uiState.join.status) {
        val status = uiState.join.status
        if (status is RoomJoinStatus.ReadyToConnect) {
            onSessionConnect(status.request)
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (sessionState is RoomSessionState.Idle) {
                RoomDiscoveryScreen(
                    uiState = uiState,
                    onServerUrlChanged = onServerUrlChanged,
                    onLoadRooms = onLoadRooms,
                    onManualRoomInputChanged = onManualRoomInputChanged,
                    onRoomSelected = onRoomSelected,
                    onDisplayNameChanged = onDisplayNameChanged,
                    onAccessPinChanged = onAccessPinChanged,
                    onCancelJoin = onCancelJoin,
                    onSubmitJoin = onSubmitJoin,
                    modifier = Modifier,
                )
            } else {
                RoomSessionScreen(
                    sessionState = sessionState,
                    displayName = displayName,
                    onVote = onVote,
                    onLeave = onLeaveSession,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SimpleEstimationAppPreview() {
    SimpleEstimationApp(
        uiState = RoomDiscoveryUiState(
            serverUrl = "https://example.com",
            status = RoomDiscoveryStatus.Idle,
        ),
        sessionState = RoomSessionState.Idle,
        displayName = null,
        onServerUrlChanged = {},
        onLoadRooms = {},
        onManualRoomInputChanged = {},
        onRoomSelected = {},
        onDisplayNameChanged = {},
        onAccessPinChanged = {},
        onCancelJoin = {},
        onSubmitJoin = {},
        onSessionConnect = {},
        onVote = { false },
        onLeaveSession = {},
    )
}
