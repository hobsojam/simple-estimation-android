package com.hobsojam.simpleestimation.feature.roomsession

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hobsojam.simpleestimation.data.websocket.ParsedRoomMessage
import com.hobsojam.simpleestimation.data.websocket.RoomJoinMessageBuilder
import com.hobsojam.simpleestimation.data.websocket.RoomSessionMessageParser
import com.hobsojam.simpleestimation.data.websocket.VoteMessageBuilder
import com.hobsojam.simpleestimation.domain.room.ReconnectScheduler
import com.hobsojam.simpleestimation.domain.room.RoomSession
import com.hobsojam.simpleestimation.domain.room.RoomSessionClient
import com.hobsojam.simpleestimation.domain.room.RoomSessionListener
import com.hobsojam.simpleestimation.domain.room.ScheduledExecutorReconnectScheduler
import com.hobsojam.simpleestimation.domain.room.SessionError
import com.hobsojam.simpleestimation.domain.room.SessionRoomState
import com.hobsojam.simpleestimation.domain.room.reconnectDelayMs
import com.hobsojam.simpleestimation.domain.server.ServerBaseUrl
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinRequest

class RoomSessionStateHolder(
    private val sessionClient: RoomSessionClient,
    private val reconnectScheduler: ReconnectScheduler = ScheduledExecutorReconnectScheduler(),
) {
    var state by mutableStateOf<RoomSessionState>(RoomSessionState.Idle)
        private set

    private var activeSession: RoomSession? = null
    private val messageParser = RoomSessionMessageParser()

    @Volatile private var connectionGeneration = 0

    @Volatile private var currentRequest: RoomJoinRequest? = null

    @Volatile private var cancelPendingReconnect: (() -> Unit)? = null

    fun connect(request: RoomJoinRequest) {
        cancelScheduledReconnect()
        currentRequest = request
        doConnect(request, attempt = 0)
    }

    val displayName: String?
        get() = currentRequest?.displayName

    fun disconnect() {
        cancelScheduledReconnect()
        currentRequest = null
        closeActiveSession()
        state = RoomSessionState.Idle
    }

    fun sendVote(vote: String): Boolean {
        if (vote !in VALID_PLANNING_POKER_VOTES) return false
        return activeSession?.let { session ->
            session.send(VoteMessageBuilder.build(vote))
            true
        } ?: false
    }

    private fun doConnect(request: RoomJoinRequest, attempt: Int) {
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
            listener = ConnectionListener(generation = generation, attempt = attempt),
        )
    }

    private fun scheduleReconnect(request: RoomJoinRequest, nextAttempt: Int) {
        val delayMs = reconnectDelayMs(nextAttempt)
        state = RoomSessionState.Reconnecting(attempt = nextAttempt, delayMs = delayMs)
        cancelPendingReconnect = reconnectScheduler.schedule(delayMs) {
            if (currentRequest === request) doConnect(request, nextAttempt)
        }
    }

    private fun cancelScheduledReconnect() {
        cancelPendingReconnect?.invoke()
        cancelPendingReconnect = null
    }

    private fun closeActiveSession() {
        activeSession?.close()
        activeSession = null
    }

    private inner class ConnectionListener(private val generation: Int, private val attempt: Int) :
        RoomSessionListener {
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
            if (!isCurrent()) return
            activeSession = null
            val request = currentRequest
            if (code == NORMAL_CLOSE_CODE || request == null) {
                state = RoomSessionState.Disconnected(userMessage = closeCodeMessage(code))
            } else {
                scheduleReconnect(request, attempt + 1)
            }
        }

        override fun onFailure(cause: Throwable) {
            if (!isCurrent()) return
            activeSession = null
            val request = currentRequest
            if (request == null) {
                state = RoomSessionState.Failed(message = cause.message ?: "Connection failed")
            } else {
                scheduleReconnect(request, attempt + 1)
            }
        }
    }
}

private val VALID_PLANNING_POKER_VOTES =
    setOf("1", "2", "3", "5", "8", "13", "21", "?", "∞", "☕")

private const val NORMAL_CLOSE_CODE = 1000
private const val GOING_AWAY_CLOSE_CODE = 1001

private fun closeCodeMessage(code: Int): String = when (code) {
    NORMAL_CLOSE_CODE -> "You have been disconnected from the room."
    GOING_AWAY_CLOSE_CODE -> "The server is going away."
    else -> "Disconnected."
}

sealed interface RoomSessionState {
    data object Idle : RoomSessionState
    data object Connecting : RoomSessionState
    data class Active(
        val roomState: SessionRoomState? = null,
        val lastError: SessionError? = null,
    ) : RoomSessionState
    data class Reconnecting(val attempt: Int, val delayMs: Long) : RoomSessionState
    data class Disconnected(val userMessage: String) : RoomSessionState
    data class Failed(val message: String) : RoomSessionState
}
