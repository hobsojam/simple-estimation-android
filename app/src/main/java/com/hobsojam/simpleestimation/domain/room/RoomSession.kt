package com.hobsojam.simpleestimation.domain.room

interface RoomSession {
    fun close()
    fun send(text: String)
}
