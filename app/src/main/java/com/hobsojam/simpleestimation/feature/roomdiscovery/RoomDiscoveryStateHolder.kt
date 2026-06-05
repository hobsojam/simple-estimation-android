package com.hobsojam.simpleestimation.feature.roomdiscovery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hobsojam.simpleestimation.domain.room.ActiveRoom
import com.hobsojam.simpleestimation.domain.room.ActiveRoomDiscoveryFailure
import com.hobsojam.simpleestimation.domain.room.ActiveRoomDiscoveryResult
import com.hobsojam.simpleestimation.domain.room.ActiveRoomRepository
import java.time.Clock
import java.time.Instant

class RoomDiscoveryStateHolder(
    private val repository: ActiveRoomRepository,
    private val clock: Clock = Clock.systemUTC(),
    initialServerUrl: String = "",
) {
    var uiState by mutableStateOf(
        RoomDiscoveryUiState(
            serverUrl = initialServerUrl,
            status = RoomDiscoveryStatus.Idle,
        ),
    )
        private set

    fun updateServerUrl(serverUrl: String) {
        uiState = uiState.copy(serverUrl = serverUrl)
    }

    suspend fun loadActiveRooms() {
        val previousRooms = uiState.status.roomsOrNull()
        uiState = uiState.copy(status = RoomDiscoveryStatus.Loading(previousRooms = previousRooms))

        uiState = when (val result = repository.loadActiveRooms(uiState.serverUrl)) {
            is ActiveRoomDiscoveryResult.Success -> {
                val loadedAt = Instant.now(clock)
                uiState.copy(
                    status = if (result.rooms.isEmpty()) {
                        RoomDiscoveryStatus.Empty(loadedAt = loadedAt)
                    } else {
                        RoomDiscoveryStatus.Loaded(
                            rooms = result.rooms,
                            loadedAt = loadedAt,
                            isStale = false,
                        )
                    },
                )
            }

            is ActiveRoomDiscoveryResult.Failure -> uiState.copy(
                status = RoomDiscoveryStatus.Error(
                    message = result.reason.toUserMessage(),
                    staleRooms = previousRooms,
                ),
            )
        }
    }

    private fun RoomDiscoveryStatus.roomsOrNull(): List<ActiveRoom>? = when (this) {
        is RoomDiscoveryStatus.Loaded -> rooms
        is RoomDiscoveryStatus.Loading -> previousRooms
        is RoomDiscoveryStatus.Error -> staleRooms
        RoomDiscoveryStatus.Idle -> null
        is RoomDiscoveryStatus.Empty -> null
    }

    private fun ActiveRoomDiscoveryFailure.toUserMessage(): String = when (this) {
        ActiveRoomDiscoveryFailure.InvalidServerUrl -> "Enter a valid server URL."
        ActiveRoomDiscoveryFailure.InsecureServerUrl -> "Use an HTTPS server URL for this build."
        ActiveRoomDiscoveryFailure.NetworkUnavailable -> "Could not reach the server. Check the URL and connection."
        ActiveRoomDiscoveryFailure.ServerUnavailable -> "The server could not load active rooms right now."
        ActiveRoomDiscoveryFailure.MalformedResponse -> "The server returned an unsupported room list."
    }
}

data class RoomDiscoveryUiState(val serverUrl: String, val status: RoomDiscoveryStatus)

sealed interface RoomDiscoveryStatus {
    data object Idle : RoomDiscoveryStatus

    data class Loading(val previousRooms: List<ActiveRoom>?) : RoomDiscoveryStatus

    data class Loaded(val rooms: List<ActiveRoom>, val loadedAt: Instant, val isStale: Boolean) :
        RoomDiscoveryStatus

    data class Empty(val loadedAt: Instant) : RoomDiscoveryStatus

    data class Error(val message: String, val staleRooms: List<ActiveRoom>?) : RoomDiscoveryStatus
}
