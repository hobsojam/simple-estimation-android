package com.hobsojam.simpleestimation.feature.serverconfig

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ServerConfigurationScreen(
    uiState: ServerConfigurationUiState,
    onServerUrlChanged: (String) -> Unit,
    onRoomIdChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onCheckCompatibilityBeforeJoin: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Join Simple Estimation",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Configure a server, then the app checks compatibility before joining a room.",
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = uiState.serverUrl,
            onValueChange = onServerUrlChanged,
            label = { Text("Server URL") },
            placeholder = { Text("https://example.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.roomId,
            onValueChange = onRoomIdChanged,
            label = { Text("Room ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.displayName,
            onValueChange = onDisplayNameChanged,
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                coroutineScope.launch { onCheckCompatibilityBeforeJoin() }
            },
            enabled = uiState.status !is ServerConfigurationStatus.Checking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(buttonText(uiState.status))
        }
        ServerConfigurationStatusMessage(
            status = uiState.status,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun ServerConfigurationStatusMessage(
    status: ServerConfigurationStatus,
    modifier: Modifier = Modifier,
) {
    when (status) {
        ServerConfigurationStatus.Idle -> Unit
        ServerConfigurationStatus.Checking -> Text(
            text = "Checking server compatibility…",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
        )

        is ServerConfigurationStatus.ReadyToJoin -> {
            if (status.demoMode) {
                Text(
                    text = "Demo mode is enabled on this server. Rooms and activity may be temporary.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = modifier,
                )
            }
            Text(
                text = status.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = if (status.demoMode) Modifier else modifier,
            )
        }

        is ServerConfigurationStatus.BlockedByUpgrade -> Text(
            text = status.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )

        is ServerConfigurationStatus.Error -> Text(
            text = status.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
    }
}

private fun buttonText(status: ServerConfigurationStatus): String =
    when (status) {
        ServerConfigurationStatus.Checking -> "Checking…"
        else -> "Check server and continue"
    }
