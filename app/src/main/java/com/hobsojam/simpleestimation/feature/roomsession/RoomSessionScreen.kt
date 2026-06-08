package com.hobsojam.simpleestimation.feature.roomsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hobsojam.simpleestimation.domain.room.PokerItemStatus
import com.hobsojam.simpleestimation.domain.room.SessionRoomState
import com.hobsojam.simpleestimation.feature.planningpoker.PlanningPokerScreen
import com.hobsojam.simpleestimation.feature.planningpoker.toUiState

@Composable
fun RoomSessionScreen(
    sessionState: RoomSessionState,
    displayName: String?,
    onVote: (String) -> Boolean,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (sessionState) {
            RoomSessionState.Idle -> Unit

            RoomSessionState.Connecting -> SessionStatusContent(
                message = "Connecting…",
                showProgress = true,
                onLeave = onLeave,
            )

            is RoomSessionState.Reconnecting -> SessionStatusContent(
                message = "Reconnecting (attempt ${sessionState.attempt})…",
                showProgress = true,
                onLeave = onLeave,
            )

            is RoomSessionState.Disconnected -> SessionStatusContent(
                message = sessionState.userMessage,
                showProgress = false,
                onLeave = onLeave,
            )

            is RoomSessionState.Failed -> SessionStatusContent(
                message = sessionState.message,
                showProgress = false,
                onLeave = onLeave,
            )

            is RoomSessionState.Active -> ActiveRoomContent(
                sessionState = sessionState,
                displayName = displayName,
                onVote = onVote,
                onLeave = onLeave,
            )
        }
    }
}

@Composable
private fun ActiveRoomContent(
    sessionState: RoomSessionState.Active,
    displayName: String?,
    onVote: (String) -> Boolean,
    onLeave: () -> Unit,
) {
    when (val roomState = sessionState.roomState) {
        null -> SessionStatusContent(
            message = "Joining room…",
            showProgress = true,
            onLeave = onLeave,
        )

        is SessionRoomState.PlanningPoker -> {
            val activeItemId = roomState.items
                .firstOrNull { it.status == PokerItemStatus.Active }?.id
            var selectedVote by remember(activeItemId) { mutableStateOf<String?>(null) }
            PlanningPokerScreen(
                state = roomState.toUiState(
                    displayName = displayName ?: "",
                    selectedVote = selectedVote,
                ),
                onVoteSelected = { vote ->
                    if (onVote(vote)) selectedVote = vote
                },
            )
        }

        is SessionRoomState.AccessProtected -> SessionStatusContent(
            message = "Access to this room is restricted.",
            showProgress = false,
            onLeave = onLeave,
        )

        else -> SessionStatusContent(
            message = "This room type is not supported in this version.",
            showProgress = false,
            onLeave = onLeave,
        )
    }
}

@Composable
private fun SessionStatusContent(message: String, showProgress: Boolean, onLeave: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            if (showProgress) {
                CircularProgressIndicator()
            }
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onLeave) {
                Text("Leave")
            }
        }
    }
}
