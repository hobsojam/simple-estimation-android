package com.hobsojam.simpleestimation.data.websocket

import com.hobsojam.simpleestimation.domain.room.RoomSession
import com.hobsojam.simpleestimation.domain.room.RoomSessionClient
import com.hobsojam.simpleestimation.domain.room.RoomSessionListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class OkHttpRoomSessionClient(private val okHttpClient: OkHttpClient = OkHttpClient()) :
    RoomSessionClient {

    override fun connect(
        url: String,
        joinMessage: String,
        listener: RoomSessionListener,
    ): RoomSession {
        val request = Request.Builder().url(url).build()
        val webSocket = okHttpClient.newWebSocket(
            request,
            SessionWebSocketListener(joinMessage = joinMessage, listener = listener),
        )
        return OkHttpRoomSession(webSocket)
    }
}

private const val NORMAL_CLOSURE_CODE = 1000

private class SessionWebSocketListener(
    private val joinMessage: String,
    private val listener: RoomSessionListener,
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocket.send(joinMessage)
        listener.onOpen()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        listener.onMessage(text)
    }

    @Suppress("UNUSED_PARAMETER")
    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        // Protocol uses text frames only
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE_CODE, null)
        listener.onClosing(code = code, reason = reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        listener.onFailure(cause = t)
    }
}

private class OkHttpRoomSession(private val webSocket: WebSocket) : RoomSession {
    override fun close() {
        webSocket.close(NORMAL_CLOSURE_CODE, null)
    }
}
