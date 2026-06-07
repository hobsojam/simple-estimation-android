package com.hobsojam.simpleestimation.feature.roomsession

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hobsojam.simpleestimation.data.websocket.RoomJoinMessageBuilder
import com.hobsojam.simpleestimation.domain.room.RoomSession
import com.hobsojam.simpleestimation.domain.room.RoomSessionClient
import com.hobsojam.simpleestimation.domain.room.RoomSessionListener
import com.hobsojam.simpleestimation.domain.server.ServerBaseUrl
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinRequest

class RoomSessionStateHolder(private val sessionClient: RoomSessionClient) {
    var state by mutableStateOf<RoomSessionState>(RoomSessionState.Idle)
        private set

    private var activeSession: RoomSession? = null

    fun connect(request: RoomJoinRequest) {
        closeActiveSession()
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
            listener = ConnectionListener(),
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

    private inner class ConnectionListener : RoomSessionListener {
        override fun onOpen() {
            state = RoomSessionState.Active
        }

        override fun onMessage(text: String) = Unit

        override fun onClosing(code: Int, reason: String) {
            state = RoomSessionState.Disconnected(code = code)
            activeSession = null
        }

        override fun onFailure(cause: Throwable) {
            state = RoomSessionState.Failed(message = cause.message ?: "Connection failed")
            activeSession = null
        }
    }
}

sealed interface RoomSessionState {
    data object Idle : RoomSessionState
    data object Connecting : RoomSessionState
    data object Active : RoomSessionState
    data class Disconnected(val code: Int) : RoomSessionState
    data class Failed(val message: String) : RoomSessionState
}
