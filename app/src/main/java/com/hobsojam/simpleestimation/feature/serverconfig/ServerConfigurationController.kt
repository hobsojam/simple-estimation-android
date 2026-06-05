package com.hobsojam.simpleestimation.feature.serverconfig

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hobsojam.simpleestimation.data.server.ServerConfigClient
import com.hobsojam.simpleestimation.domain.server.ProtocolCompatibility
import com.hobsojam.simpleestimation.domain.server.ProtocolCompatibilityChecker
import com.hobsojam.simpleestimation.domain.server.ServerBaseUrl
import com.hobsojam.simpleestimation.domain.server.ServerConfig

class ServerConfigurationController(
    private val configClient: ServerConfigClient,
    private val cleartextAllowed: Boolean,
) {
    var uiState by mutableStateOf(ServerConfigurationUiState())
        private set

    fun onServerUrlChanged(value: String) {
        uiState = uiState.copy(serverUrl = value, status = ServerConfigurationStatus.Idle)
    }

    fun onRoomIdChanged(value: String) {
        uiState = uiState.copy(roomId = value, status = ServerConfigurationStatus.Idle)
    }

    fun onDisplayNameChanged(value: String) {
        uiState = uiState.copy(displayName = value, status = ServerConfigurationStatus.Idle)
    }

    suspend fun checkCompatibilityBeforeJoin() {
        val baseUrl = resolveBaseUrl() ?: return
        if (!validateFields()) return
        uiState = uiState.copy(
            normalizedServerUrl = baseUrl.value,
            normalizedWebSocketUrl = baseUrl.webSocketEndpoint(),
            status = ServerConfigurationStatus.Checking,
        )
        configClient.fetchConfig(baseUrl).fold(
            onSuccess = { config -> uiState = buildCompatibilityUiState(config) },
            onFailure = { exception -> showError(exception.userSafeMessage()) },
        )
    }

    private fun resolveBaseUrl(): ServerBaseUrl? {
        val parsed = ServerBaseUrl.parse(uiState.serverUrl, cleartextAllowed)
        parsed.onFailure { showError(it.userSafeMessage()) }
        return parsed.getOrNull()
    }

    private fun validateFields(): Boolean {
        val roomId = uiState.roomId.trim()
        val displayName = uiState.displayName.trim()
        return when {
            roomId.isEmpty() -> {
                showError("Enter a room ID before continuing.")
                false
            }
            displayName.isEmpty() -> {
                showError("Enter a display name before continuing.")
                false
            }
            else -> true
        }
    }

    private fun buildCompatibilityUiState(config: ServerConfig): ServerConfigurationUiState =
        when (val compatibility = ProtocolCompatibilityChecker.check(config)) {
            is ProtocolCompatibility.Compatible -> uiState.copy(
                status = ServerConfigurationStatus.ReadyToJoin(
                    demoMode = compatibility.config.demoMode,
                    message = "Protocol version ${compatibility.config.protocolVersion} is supported. " +
                        "Ready to join room ${uiState.roomId.trim()}.",
                ),
            )
            is ProtocolCompatibility.Unsupported -> uiState.copy(
                status = ServerConfigurationStatus.BlockedByUpgrade(
                    message = compatibility.message,
                ),
            )
        }

    private fun showError(message: String) {
        uiState = uiState.copy(status = ServerConfigurationStatus.Error(message))
    }

    private fun Throwable.userSafeMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
}

data class ServerConfigurationUiState(
    val serverUrl: String = "",
    val roomId: String = "",
    val displayName: String = "",
    val normalizedServerUrl: String? = null,
    val normalizedWebSocketUrl: String? = null,
    val status: ServerConfigurationStatus = ServerConfigurationStatus.Idle,
)

sealed interface ServerConfigurationStatus {
    data object Idle : ServerConfigurationStatus
    data object Checking : ServerConfigurationStatus

    data class ReadyToJoin(val demoMode: Boolean, val message: String) : ServerConfigurationStatus

    data class BlockedByUpgrade(val message: String) : ServerConfigurationStatus

    data class Error(val message: String) : ServerConfigurationStatus
}
