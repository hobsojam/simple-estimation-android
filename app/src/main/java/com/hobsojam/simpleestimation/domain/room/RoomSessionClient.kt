package com.hobsojam.simpleestimation.domain.room

interface RoomSessionClient {
    fun connect(url: String, joinMessage: String, listener: RoomSessionListener): RoomSession
}

interface RoomSessionListener {
    fun onOpen()
    fun onMessage(text: String)
    fun onClosing(code: Int, reason: String)
    fun onFailure(cause: Throwable)
}
