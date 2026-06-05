package com.hobsojam.simpleestimation.domain.room

interface ActiveRoomRepository {
    suspend fun loadActiveRooms(serverBaseUrl: String): ActiveRoomDiscoveryResult
}

sealed interface ActiveRoomDiscoveryResult {
    data class Success(val rooms: List<ActiveRoom>) : ActiveRoomDiscoveryResult

    data class Failure(val reason: ActiveRoomDiscoveryFailure) : ActiveRoomDiscoveryResult
}

enum class ActiveRoomDiscoveryFailure {
    InvalidServerUrl,
    InsecureServerUrl,
    NetworkUnavailable,
    ServerUnavailable,
    MalformedResponse,
}
