package com.hobsojam.simpleestimation.feature.roomdiscovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun RoomJoinPanel(
    joinState: RoomJoinUiState,
    onManualRoomInputChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onAccessPinChanged: (String) -> Unit,
    onCancelJoin: () -> Unit,
    onSubmitJoin: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Join a room",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text("Paste a room link, enter a room ID, or choose a room from the active list.")
            val joiningRoom = joinState.mode as? RoomJoinMode.JoiningRoom
            OutlinedTextField(
                value = joiningRoom?.roomIdInput.orEmpty(),
                onValueChange = onManualRoomInputChanged,
                label = { Text("Room link or ID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            joiningRoom?.roomName?.let { roomName -> Text("Selected room: $roomName") }
            OutlinedTextField(
                value = joinState.displayName,
                onValueChange = onDisplayNameChanged,
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (joiningRoom != null) {
                OutlinedTextField(
                    value = joinState.accessPin,
                    onValueChange = onAccessPinChanged,
                    label = { Text(accessPinLabel(joiningRoom.accessPinRequired)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            RoomJoinStatusMessage(status = joinState.status)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onSubmitJoin,
                    modifier = Modifier.semantics { role = Role.Button },
                ) {
                    Text("Join room")
                }
                OutlinedButton(
                    onClick = onCancelJoin,
                    modifier = Modifier.semantics { role = Role.Button },
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun RoomJoinStatusMessage(status: RoomJoinStatus) {
    when (status) {
        RoomJoinStatus.Idle -> Unit
        RoomJoinStatus.CheckingCompatibility -> Text(
            text = "Checking server compatibility…",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
        )
        is RoomJoinStatus.Error -> Text(
            text = status.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
        is RoomJoinStatus.ReadyToConnect -> Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (status.demoMode) {
                Text(
                    text = "Demo mode is enabled on this server. " +
                        "Room data may reset without notice.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text(
                text = "Ready to connect as ${status.request.displayName}.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun accessPinLabel(accessPinRequired: Boolean): String =
    if (accessPinRequired) "Access PIN" else "Access PIN (if required)"
