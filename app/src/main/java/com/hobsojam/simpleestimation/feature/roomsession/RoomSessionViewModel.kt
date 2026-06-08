package com.hobsojam.simpleestimation.feature.roomsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hobsojam.simpleestimation.data.websocket.OkHttpRoomSessionClient
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinRequest

class RoomSessionViewModel(
    private val stateHolder: RoomSessionStateHolder = RoomSessionStateHolder(
        OkHttpRoomSessionClient(),
    ),
) : ViewModel() {

    val sessionState: RoomSessionState
        get() = stateHolder.state

    val displayName: String?
        get() = stateHolder.displayName

    fun connect(request: RoomJoinRequest) {
        stateHolder.connect(request)
    }

    fun disconnect() {
        stateHolder.disconnect()
    }

    fun sendVote(vote: String): Boolean = stateHolder.sendVote(vote)

    override fun onCleared() {
        stateHolder.disconnect()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RoomSessionViewModel::class.java)) {
                return RoomSessionViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
