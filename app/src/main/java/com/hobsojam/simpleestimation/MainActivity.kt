package com.hobsojam.simpleestimation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hobsojam.simpleestimation.data.room.HttpActiveRoomRepository
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryScreen
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryStatus
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryUiState
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryViewModel

class MainActivity : ComponentActivity() {
    private val roomDiscoveryViewModel: RoomDiscoveryViewModel by viewModels {
        RoomDiscoveryViewModel.Factory(
            repositoryFactory = { HttpActiveRoomRepository() },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SimpleEstimationApp(
                uiState = roomDiscoveryViewModel.uiState,
                onServerUrlChanged = roomDiscoveryViewModel::updateServerUrl,
                onLoadRooms = roomDiscoveryViewModel::loadActiveRooms,
            )
        }
    }
}

@Composable
fun SimpleEstimationApp(
    uiState: RoomDiscoveryUiState,
    onServerUrlChanged: (String) -> Unit,
    onLoadRooms: () -> Unit,
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            RoomDiscoveryScreen(
                uiState = uiState,
                onServerUrlChanged = onServerUrlChanged,
                onLoadRooms = onLoadRooms,
                modifier = Modifier,
            )
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
        onServerUrlChanged = {},
        onLoadRooms = {},
    )
}
