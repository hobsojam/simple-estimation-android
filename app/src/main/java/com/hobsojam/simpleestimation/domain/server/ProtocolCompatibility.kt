package com.hobsojam.simpleestimation.domain.server

sealed interface ProtocolCompatibility {
    data class Compatible(val config: ServerConfig) : ProtocolCompatibility

    data class Unsupported(val protocolVersion: Int, val message: String) : ProtocolCompatibility
}

object ProtocolCompatibilityChecker {
    const val SUPPORTED_PROTOCOL_VERSION = 1

    fun check(config: ServerConfig): ProtocolCompatibility = if (config.protocolVersion == SUPPORTED_PROTOCOL_VERSION) {
        ProtocolCompatibility.Compatible(config)
    } else {
        ProtocolCompatibility.Unsupported(
            protocolVersion = config.protocolVersion,
            message = "This server uses protocol version ${config.protocolVersion}. " +
                "Update Simple Estimation for Android to join rooms on this server.",
        )
    }
}
