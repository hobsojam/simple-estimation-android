package com.hobsojam.simpleestimation.feature.roomdiscovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hobsojam.simpleestimation.domain.room.ActiveRoomRepository
import kotlinx.coroutines.launch

class RoomDiscoveryViewModel(repository: ActiveRoomRepository) : ViewModel() {
    private val stateHolder = RoomDiscoveryStateHolder(repository = repository)

    val uiState: RoomDiscoveryUiState
        get() = stateHolder.uiState

    fun updateServerUrl(serverUrl: String) {
        stateHolder.updateServerUrl(serverUrl)
    }

    fun loadActiveRooms() {
        viewModelScope.launch {
            stateHolder.loadActiveRooms()
        }
    }

    class Factory(private val repositoryFactory: () -> ActiveRoomRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RoomDiscoveryViewModel::class.java)) {
                return RoomDiscoveryViewModel(repository = repositoryFactory()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
