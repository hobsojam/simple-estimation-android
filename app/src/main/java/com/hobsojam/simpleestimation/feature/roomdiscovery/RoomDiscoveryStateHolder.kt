package com.hobsojam.simpleestimation.feature.roomdiscovery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hobsojam.simpleestimation.data.server.JavaNetServerConfigClient
import com.hobsojam.simpleestimation.data.server.ServerConfigClient
import com.hobsojam.simpleestimation.data.server.ServerConfigParseException
import com.hobsojam.simpleestimation.domain.room.ActiveRoom
import com.hobsojam.simpleestimation.domain.room.ActiveRoomDiscoveryFailure
import com.hobsojam.simpleestimation.domain.room.ActiveRoomDiscoveryResult
import com.hobsojam.simpleestimation.domain.room.ActiveRoomRepository
import com.hobsojam.simpleestimation.domain.server.ProtocolCompatibility
import com.hobsojam.simpleestimation.domain.server.ProtocolCompatibilityChecker
import com.hobsojam.simpleestimation.domain.server.ServerBaseUrl
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.UUID

class RoomDiscoveryStateHolder(
    private val repository: ActiveRoomRepository,
    private val configClient: ServerConfigClient = JavaNetServerConfigClient(),
    private val clock: Clock = Clock.systemUTC(),
    initialServerUrl: String = "",
    private val cleartextAllowed: Boolean = false,
    private val participantIdGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    private val participantSessionStore = ParticipantSessionStore(participantIdGenerator)

    var uiState by mutableStateOf(
        RoomDiscoveryUiState(
            serverUrl = initialServerUrl,
            status = RoomDiscoveryStatus.Idle,
            join = RoomJoinUiState(),
        ),
    )
        private set

    fun updateServerUrl(serverUrl: String) {
        uiState = uiState.copy(
            serverUrl = serverUrl,
            join = uiState.join.copy(accessPin = "", status = RoomJoinStatus.Idle),
        )
    }

    fun updateManualRoomInput(value: String) {
        val input = parseRoomInput(value) ?: RoomInput(roomId = value, serverBaseUrl = null)
        uiState = uiState.copy(
            serverUrl = input.serverBaseUrl ?: uiState.serverUrl,
            join = uiState.join.copy(
                mode = RoomJoinMode.JoiningRoom(
                    roomIdInput = input.roomId,
                    roomName = null,
                    accessPinRequired = false,
                ),
                accessPin = "",
                status = RoomJoinStatus.Idle,
            ),
        )
    }

    fun openRoomLink(value: String) {
        val input = parseRoomInput(value)
        uiState = if (input == null) {
            uiState.copy(
                join = RoomJoinUiState(
                    displayName = uiState.join.displayName,
                    status = RoomJoinStatus.Error(INVALID_ROOM_LINK_MESSAGE),
                ),
            )
        } else {
            uiState.copy(
                serverUrl = input.serverBaseUrl ?: uiState.serverUrl,
                join = uiState.join.copy(
                    mode = RoomJoinMode.JoiningRoom(
                        roomIdInput = input.roomId,
                        roomName = null,
                        accessPinRequired = false,
                    ),
                    accessPin = "",
                    status = RoomJoinStatus.Idle,
                ),
            )
        }
    }

    fun selectRoom(room: ActiveRoom) {
        uiState = uiState.copy(
            join = uiState.join.copy(
                mode = RoomJoinMode.JoiningRoom(
                    roomIdInput = room.id,
                    roomName = room.name,
                    accessPinRequired = room.accessPinProtected,
                ),
                accessPin = "",
                status = RoomJoinStatus.Idle,
            ),
        )
    }

    fun updateDisplayName(value: String) {
        uiState = uiState.copy(
            join = uiState.join.copy(
                displayName = value,
                status = RoomJoinStatus.Idle,
            ),
        )
    }

    fun updateAccessPin(value: String) {
        uiState = uiState.copy(
            join = uiState.join.copy(
                accessPin = value,
                status = RoomJoinStatus.Idle,
            ),
        )
    }

    fun cancelJoin() {
        uiState = uiState.copy(join = RoomJoinUiState(displayName = uiState.join.displayName))
    }

    suspend fun submitJoin() {
        val validation = validateJoinInput()
        if (validation is JoinValidationResult.Invalid) {
            uiState = uiState.copy(join = uiState.join.copy(status = RoomJoinStatus.Error(validation.message)))
            return
        }
        val validInput = validation as JoinValidationResult.Valid
        uiState = uiState.copy(join = uiState.join.copy(status = RoomJoinStatus.CheckingCompatibility))

        configClient.fetchConfig(validInput.serverBaseUrl).fold(
            onSuccess = { config ->
                when (val compatibility = ProtocolCompatibilityChecker.check(config)) {
                    is ProtocolCompatibility.Compatible -> markReadyToConnect(
                        validInput = validInput,
                        demoMode = compatibility.config.demoMode,
                    )
                    is ProtocolCompatibility.Unsupported -> {
                        uiState = uiState.copy(
                            join = uiState.join.copy(status = RoomJoinStatus.Error(compatibility.message)),
                        )
                    }
                }
            },
            onFailure = { exception ->
                uiState = uiState.copy(
                    join = uiState.join.copy(status = RoomJoinStatus.Error(exception.toJoinErrorMessage())),
                )
            },
        )
    }

    private fun validateJoinInput(): JoinValidationResult {
        val joiningRoom = uiState.join.mode as? RoomJoinMode.JoiningRoom
        val roomId = joiningRoom?.roomIdInput?.trim().orEmpty()
        val serverUrl = uiState.serverUrl.trim()
        val displayName = uiState.join.displayName.trim()
        val accessPin = uiState.join.accessPin.trim()
        val serverBaseUrl = ServerBaseUrl.parse(serverUrl, cleartextAllowed)
        val errorMessage = when {
            serverUrl.isEmpty() -> "Enter a server URL before joining."
            serverBaseUrl.isFailure -> serverBaseUrl.exceptionOrNull()?.message ?: "Enter a valid server URL."
            roomId.isEmpty() || roomId.length > MAX_SHARED_TEXT_LENGTH || roomId.any(Char::isWhitespace) -> {
                INVALID_ROOM_LINK_MESSAGE
            }
            displayName.isEmpty() -> "Enter a display name before joining."
            displayName.length > MAX_SHARED_TEXT_LENGTH -> "Display name must be 200 characters or fewer."
            joiningRoom?.accessPinRequired == true && accessPin.isEmpty() -> {
                "Enter the room access PIN to continue."
            }
            accessPin.length > MAX_PIN_LENGTH -> "Access PIN must be 64 characters or fewer."
            else -> null
        }
        return if (errorMessage == null) {
            JoinValidationResult.Valid(
                serverBaseUrl = serverBaseUrl.getOrThrow(),
                roomId = roomId,
                displayName = displayName,
                accessPin = accessPin.takeIf { it.isNotEmpty() },
            )
        } else {
            JoinValidationResult.Invalid(errorMessage)
        }
    }

    private fun markReadyToConnect(
        validInput: JoinValidationResult.Valid,
        demoMode: Boolean,
    ) {
        val validatedServerBaseUrl = validInput.serverBaseUrl.value
        uiState = uiState.copy(
            join = uiState.join.copy(
                status = RoomJoinStatus.ReadyToConnect(
                    request = RoomJoinRequest(
                        serverBaseUrl = validatedServerBaseUrl,
                        roomId = validInput.roomId,
                        participantId = participantSessionStore.participantIdFor(
                            serverBaseUrl = validatedServerBaseUrl,
                            roomId = validInput.roomId,
                        ),
                        displayName = validInput.displayName,
                        accessPin = validInput.accessPin,
                    ),
                    demoMode = demoMode,
                ),
            ),
        )
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

}

private fun RoomDiscoveryStatus.roomsOrNull(): List<ActiveRoom>? =
    when (this) {
        is RoomDiscoveryStatus.Loaded -> rooms
        is RoomDiscoveryStatus.Loading -> previousRooms
        is RoomDiscoveryStatus.Error -> staleRooms
        RoomDiscoveryStatus.Idle -> null
        is RoomDiscoveryStatus.Empty -> null
    }

private fun ActiveRoomDiscoveryFailure.toUserMessage(): String =
    when (this) {
        ActiveRoomDiscoveryFailure.InvalidServerUrl -> "Enter a valid server URL."
        ActiveRoomDiscoveryFailure.InsecureServerUrl -> "Use an HTTPS server URL for this build."
        ActiveRoomDiscoveryFailure.NetworkUnavailable -> "Could not reach the server. Check the URL and connection."
        ActiveRoomDiscoveryFailure.ServerUnavailable -> "The server could not load active rooms right now."
        ActiveRoomDiscoveryFailure.MalformedResponse -> "The server returned an unsupported room list."
    }

private fun Throwable.toJoinErrorMessage(): String = when (this) {
    is ServerConfigParseException ->
        "The server returned an unsupported configuration. Update Simple Estimation or try another server."
    else ->
        message?.takeIf { it.isNotBlank() }
            ?: "Could not check server compatibility. Check the server URL and try again."
}

data class RoomDiscoveryUiState(
    val serverUrl: String,
    val status: RoomDiscoveryStatus,
    val join: RoomJoinUiState = RoomJoinUiState(),
)

sealed interface RoomDiscoveryStatus {
    data object Idle : RoomDiscoveryStatus

    data class Loading(val previousRooms: List<ActiveRoom>?) : RoomDiscoveryStatus

    data class Loaded(
        val rooms: List<ActiveRoom>,
        val loadedAt: Instant,
        val isStale: Boolean,
    ) : RoomDiscoveryStatus

    data class Empty(val loadedAt: Instant) : RoomDiscoveryStatus

    data class Error(
        val message: String,
        val staleRooms: List<ActiveRoom>?,
    ) : RoomDiscoveryStatus
}

data class RoomJoinUiState(
    val mode: RoomJoinMode = RoomJoinMode.ManualEntry,
    val displayName: String = "",
    val accessPin: String = "",
    val status: RoomJoinStatus = RoomJoinStatus.Idle,
)

sealed interface RoomJoinMode {
    data object ManualEntry : RoomJoinMode

    data class JoiningRoom(
        val roomIdInput: String,
        val roomName: String?,
        val accessPinRequired: Boolean,
    ) : RoomJoinMode
}

sealed interface RoomJoinStatus {
    data object Idle : RoomJoinStatus

    data object CheckingCompatibility : RoomJoinStatus

    data class ReadyToConnect(
        val request: RoomJoinRequest,
        val demoMode: Boolean,
    ) : RoomJoinStatus

    data class Error(val message: String) : RoomJoinStatus
}

data class RoomJoinRequest(
    val serverBaseUrl: String,
    val roomId: String,
    val participantId: String,
    val displayName: String,
    val accessPin: String?,
)

private sealed interface JoinValidationResult {
    data class Valid(
        val serverBaseUrl: ServerBaseUrl,
        val roomId: String,
        val displayName: String,
        val accessPin: String?,
    ) : JoinValidationResult

    data class Invalid(val message: String) : JoinValidationResult
}

private data class ParticipantSessionKey(
    val serverBaseUrl: String,
    val roomId: String,
)

private class ParticipantSessionStore(
    private val participantIdGenerator: () -> String,
) {
    private val participantIdsByRoom = mutableMapOf<ParticipantSessionKey, String>()

    fun participantIdFor(
        serverBaseUrl: String,
        roomId: String,
    ): String =
        participantIdsByRoom.getOrPut(
            ParticipantSessionKey(
                serverBaseUrl = serverBaseUrl,
                roomId = roomId,
            ),
            participantIdGenerator,
        )
}

private data class RoomInput(
    val roomId: String,
    val serverBaseUrl: String?,
)

private const val MAX_SHARED_TEXT_LENGTH = 200
private const val MAX_PIN_LENGTH = 64
private const val INVALID_ROOM_LINK_MESSAGE = "Enter a valid room link or room ID."
private const val HTTP_SCHEME = "http"
private const val HTTPS_SCHEME = "https"
private const val ROOM_QUERY_PARAMETER = "room"
private const val ROOT_PATH = "/"

private fun parseRoomInput(value: String): RoomInput? {
    val trimmed = value.trim()
    val sharedLink = trimmed
        .takeUnless { it.isEmpty() }
        ?.split(Regex("\\s+"))
        ?.firstOrNull { it.contains("?room=") || it.contains("&room=") }
    return parseRoomLink(trimmed)
        ?: sharedLink?.let(::parseRoomLink)
        ?: parseManualRoomId(trimmed)
}

private fun parseManualRoomId(value: String): RoomInput? {
    val uri = runCatching { URI(value) }.getOrNull()
    return RoomInput(
        roomId = value,
        serverBaseUrl = null,
    ).takeUnless {
        value.isEmpty() || value.any(Char::isWhitespace) || !uri?.scheme.isNullOrBlank()
    }
}

private fun parseRoomLink(value: String): RoomInput? {
    val uri = runCatching { URI(value) }.getOrNull()
    return uri
        ?.takeIf { it.scheme in setOf(HTTP_SCHEME, HTTPS_SCHEME) && it.userInfo == null && !it.host.isNullOrBlank() }
        ?.rawQuery
        ?.split('&')
        ?.firstNotNullOfOrNull { parameter -> parameter.toRoomQueryValue() }
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { roomId ->
            RoomInput(
                roomId = roomId,
                serverBaseUrl = uri.toServerBaseUrl(),
            )
        }
}

private fun String.toRoomQueryValue(): String? {
    val parts = split('=', limit = 2)
    val name = parts.firstOrNull()?.decodeUrlComponentOrNull()
    val parameterValue = parts.getOrNull(1)?.decodeUrlComponentOrNull()
    return parameterValue?.takeIf { name == ROOM_QUERY_PARAMETER }
}

private fun URI.toServerBaseUrl(): String =
    URI(
        scheme.lowercase(),
        null,
        host,
        port,
        path.takeUnless { it.isNullOrBlank() || it == ROOT_PATH },
        null,
        null,
    ).toString().trimEnd('/')

private fun String.decodeUrlComponentOrNull(): String? =
    runCatching { URLDecoder.decode(this, StandardCharsets.UTF_8.name()) }.getOrNull()
