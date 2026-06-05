package com.hobsojam.simpleestimation.domain.server

import java.net.URI

data class ServerBaseUrl private constructor(
    val value: String,
) {
    fun configEndpoint(): String = "$value/api/config"

    fun webSocketEndpoint(): String {
        val uri = URI(value)
        val wsScheme = when (uri.scheme) {
            HTTPS_SCHEME -> WSS_SCHEME
            HTTP_SCHEME -> WS_SCHEME
            else -> error("Unsupported server URL scheme")
        }
        return URI(
            wsScheme,
            uri.userInfo,
            uri.host,
            uri.port,
            WS_PATH,
            null,
            null,
        ).toString()
    }

    companion object {
        private const val HTTPS_SCHEME = "https"
        private const val HTTP_SCHEME = "http"
        private const val WSS_SCHEME = "wss"
        private const val WS_SCHEME = "ws"
        private const val WS_PATH = "/ws"

        fun parse(
            rawValue: String,
            cleartextAllowed: Boolean,
        ): Result<ServerBaseUrl> {
            val trimmed = rawValue.trim().trimEnd('/')
            if (trimmed.isEmpty()) {
                return Result.failure(ServerBaseUrlException("Enter a server URL before continuing."))
            }

            val uri = runCatching { URI(trimmed) }
                .getOrElse {
                    return Result.failure(ServerBaseUrlException("Enter a valid server URL."))
                }

            val scheme = uri.scheme?.lowercase()
            if (scheme != HTTPS_SCHEME && scheme != HTTP_SCHEME) {
                return Result.failure(ServerBaseUrlException("Server URL must start with https://."))
            }
            if (uri.host.isNullOrBlank()) {
                return Result.failure(ServerBaseUrlException("Server URL must include a host name."))
            }
            if (uri.rawQuery != null || uri.rawFragment != null) {
                return Result.failure(ServerBaseUrlException("Server URL cannot include query text or a fragment."))
            }
            if (scheme == HTTP_SCHEME && !cleartextAllowed) {
                return Result.failure(
                    ServerBaseUrlException("Release builds require an HTTPS Simple Estimation server."),
                )
            }

            val normalized = URI(
                scheme,
                uri.userInfo,
                uri.host,
                uri.port,
                uri.path.takeUnless { it.isNullOrBlank() || it == "/" },
                null,
                null,
            ).toString().trimEnd('/')

            return Result.success(ServerBaseUrl(normalized))
        }
    }
}

class ServerBaseUrlException(message: String) : IllegalArgumentException(message)
