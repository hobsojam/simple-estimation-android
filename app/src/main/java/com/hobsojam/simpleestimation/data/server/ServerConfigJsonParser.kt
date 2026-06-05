package com.hobsojam.simpleestimation.data.server

import com.hobsojam.simpleestimation.domain.server.ServerConfig

object ServerConfigJsonParser {
    private val demoModePattern = Regex(""""demoMode"\s*:\s*(true|false)""")
    private val protocolVersionPattern = Regex(""""protocolVersion"\s*:\s*(-?\d+)""")

    fun parse(json: String): Result<ServerConfig> {
        val demoMode = demoModePattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        val protocolVersion = protocolVersionPattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
        return when {
            demoMode == null -> Result.failure(ServerConfigParseException("Server configuration is missing demo mode."))
            protocolVersion == null -> Result.failure(ServerConfigParseException("Server configuration is missing protocol version."))
            else -> Result.success(ServerConfig(demoMode = demoMode, protocolVersion = protocolVersion))
        }
    }
}

class ServerConfigParseException(message: String) : IllegalArgumentException(message)
