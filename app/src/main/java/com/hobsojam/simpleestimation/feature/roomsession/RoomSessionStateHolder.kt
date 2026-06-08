package com.hobsojam.simpleestimation.feature.roomsession

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hobsojam.simpleestimation.data.websocket.ParsedRoomMessage
import com.hobsojam.simpleestimation.data.websocket.RoomJoinMessageBuilder
import com.hobsojam.simpleestimation.data.websocket.RoomSessionMessageParser
import com.hobsojam.simpleestimation.domain.room.RoomSession
import com.hobsojam.simpleestimation.domain.room.RoomSessionClient
import com.hobsojam.simpleestimation.domain.room.RoomSessionListener
import com.hobsojam.simpleestimation.domain.room.SessionError
import com.hobsojam.simpleestimation.domain.room.SessionRoomState
import com.hobsojam.simpleestimation.domain.server.ServerBaseUrl
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinRequest

class RoomSessionStateHolder(private val sessionClient: RoomSessionClient) {
    var state by mutableStateOf<RoomSessionState>(RoomSessionState.Idle)
        private set

    private var activeSession: RoomSession? = null
    private val messageParser = RoomSessionMessageParser()

    @Volatile private var connectionGeneration = 0

    fun connect(request: RoomJoinRequest) {
        closeActiveSession()
        val generation = ++connectionGeneration
        state = RoomSessionState.Connecting
        val url = ServerBaseUrl.fromValidated(request.serverBaseUrl)
            .webSocketSessionUrl(
                roomId = request.roomId,
                participantId = request.participantId,
            )
        val joinMessage = RoomJoinMessageBuilder.build(
            displayName = request.displayName,
            accessPin = request.accessPin,
        )
        activeSession = sessionClient.connect(
            url = url,
            joinMessage = joinMessage,
            listener = ConnectionListener(generation),
        )
    }

    fun disconnect() {
        closeActiveSession()
        state = RoomSessionState.Idle
    }

    private fun closeActiveSession() {
        activeSession?.close()
        activeSession = null
    }

    private inner class ConnectionListener(private val generation: Int) : RoomSessionListener {
        private fun isCurrent() = generation == connectionGeneration

        override fun onOpen() {
            if (isCurrent()) state = RoomSessionState.Active()
        }

        override fun onMessage(text: String) {
            if (!isCurrent()) return
            val current = state as? RoomSessionState.Active ?: return
            messageParser.parse(text).onSuccess { parsed ->
                state = when (parsed) {
                    is ParsedRoomMessage.RoomState ->
                        current.copy(roomState = parsed.state, lastError = null)
                    is ParsedRoomMessage.ServerError ->
                        current.copy(lastError = parsed.error)
                }
            }
        }

        override fun onClosing(code: Int, reason: String) {
            if (isCurrent()) {
                state = RoomSessionState.Disconnected(code = code)
                activeSession = null
            }
        }

        override fun onFailure(cause: Throwable) {
            if (isCurrent()) {
                state = RoomSessionState.Failed(message = cause.message ?: "Connection failed")
                activeSession = null
            }
        }
    }
}

sealed interface RoomSessionState {
    data object Idle : RoomSessionState
    data object Connecting : RoomSessionState
    data class Active(
        val roomState: SessionRoomState? = null,
        val lastError: SessionError? = null,
    ) : RoomSessionState
    data class Disconnected(val code: Int) : RoomSessionState
    data class Failed(val message: String) : RoomSessionState
}
