package com.hobsojam.simpleestimation.feature.roomdiscovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hobsojam.simpleestimation.domain.room.ActiveRoom
import com.hobsojam.simpleestimation.domain.room.EstimationRoomType
import java.time.Instant

@Composable
fun RoomDiscoveryScreen(
    uiState: RoomDiscoveryUiState,
    onServerUrlChanged: (String) -> Unit,
    onLoadRooms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoomDiscoveryContent(
        uiState = uiState,
        onServerUrlChanged = onServerUrlChanged,
        onLoadRooms = onLoadRooms,
        modifier = modifier,
    )
}

@Composable
private fun RoomDiscoveryContent(
    uiState: RoomDiscoveryUiState,
    onServerUrlChanged: (String) -> Unit,
    onLoadRooms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Active rooms",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Load participant rooms from your configured Simple Estimation server.",
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = uiState.serverUrl,
            onValueChange = onServerUrlChanged,
            label = { Text("Server URL") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onLoadRooms,
            enabled = uiState.status !is RoomDiscoveryStatus.Loading,
            modifier = Modifier.semantics { role = Role.Button },
        ) {
            Text(loadButtonText(uiState.status))
        }
        RoomDiscoveryStatusContent(status = uiState.status)
    }
}

private const val REFRESH_ROOM_LIST_BUTTON_TEXT = "Refresh room list"

private fun loadButtonText(status: RoomDiscoveryStatus): String = when (status) {
    RoomDiscoveryStatus.Idle -> "Load active rooms"
    is RoomDiscoveryStatus.Loading -> "Loading rooms"
    is RoomDiscoveryStatus.Empty -> REFRESH_ROOM_LIST_BUTTON_TEXT
    is RoomDiscoveryStatus.Error -> REFRESH_ROOM_LIST_BUTTON_TEXT
    is RoomDiscoveryStatus.Loaded -> REFRESH_ROOM_LIST_BUTTON_TEXT
}

@Composable
private fun RoomDiscoveryStatusContent(status: RoomDiscoveryStatus) {
    when (status) {
        RoomDiscoveryStatus.Idle -> Text("Enter a server URL, then load active rooms.")
        is RoomDiscoveryStatus.Empty -> Text("No active rooms are available on this server.")
        is RoomDiscoveryStatus.Loading -> LoadingRooms(previousRooms = status.previousRooms)
        is RoomDiscoveryStatus.Loaded -> RoomList(
            rooms = status.rooms,
            staleMessage = if (status.isStale) "Showing a stale room list." else null,
        )
        is RoomDiscoveryStatus.Error -> ErrorRooms(status = status)
    }
}

@Composable
private fun LoadingRooms(previousRooms: List<ActiveRoom>?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator()
        Text("Loading active rooms…")
    }
    previousRooms?.takeIf { it.isNotEmpty() }?.let { rooms ->
        Text("Refreshing. Showing the previous room list until the server responds.")
        RoomList(rooms = rooms, staleMessage = "Previous results may be stale.")
    }
}

@Composable
private fun ErrorRooms(status: RoomDiscoveryStatus.Error) {
    Text(
        text = status.message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
    )
    status.staleRooms?.takeIf { it.isNotEmpty() }?.let { rooms ->
        RoomList(
            rooms = rooms,
            staleMessage = "Showing stale room results from the last successful load.",
        )
    }
}

@Composable
private fun RoomList(rooms: List<ActiveRoom>, staleMessage: String?) {
    staleMessage?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
        )
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items = rooms, key = { it.id }) { room ->
            RoomCard(room = room)
        }
    }
}

@Composable
private fun RoomCard(room: ActiveRoom) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = room.name ?: "Unnamed room",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(room.type.displayName)
            Text("${room.participantCount} participants")
            if (room.accessPinProtected) {
                Text("Access PIN required")
            }
            if (room.pinProtected) {
                Text("Facilitator PIN protected")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RoomDiscoveryContentPreview() {
    MaterialTheme {
        RoomDiscoveryContent(
            uiState = RoomDiscoveryUiState(
                serverUrl = "https://example.com",
                status = RoomDiscoveryStatus.Loaded(
                    rooms = listOf(
                        ActiveRoom(
                            id = "room-1",
                            type = EstimationRoomType.PlanningPoker,
                            name = "Sprint planning",
                            participantCount = 3,
                            pinProtected = true,
                            accessPinProtected = false,
                        ),
                    ),
                    loadedAt = Instant.EPOCH,
                    isStale = false,
                ),
            ),
            onServerUrlChanged = {},
            onLoadRooms = {},
        )
    }
}
