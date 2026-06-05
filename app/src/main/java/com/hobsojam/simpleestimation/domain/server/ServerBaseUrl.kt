package com.hobsojam.simpleestimation.domain.server

import java.net.URI

data class ServerBaseUrl private constructor(val value: String) {
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

        fun parse(rawValue: String, cleartextAllowed: Boolean): Result<ServerBaseUrl> {
            val trimmed = rawValue.trim().trimEnd('/')
            return parseUri(trimmed).fold(
                onFailure = { Result.failure(it) },
                onSuccess = { uri -> buildFromUri(uri, cleartextAllowed) },
            )
        }

        private fun parseUri(trimmed: String): Result<URI> = when {
            trimmed.isEmpty() -> Result.failure(ServerBaseUrlException("Enter a server URL before continuing."))
            else -> runCatching { URI(trimmed) }
                .recoverCatching { throw ServerBaseUrlException("Enter a valid server URL.") }
        }

        private fun buildFromUri(uri: URI, cleartextAllowed: Boolean): Result<ServerBaseUrl> {
            val scheme = uri.scheme?.lowercase()
            val error = checkScheme(scheme)
                ?: checkHost(uri)
                ?: checkQueryOrFragment(uri)
                ?: checkCleartextAllowed(scheme, cleartextAllowed)
            return if (error != null) Result.failure(error) else Result.success(buildNormalized(scheme!!, uri))
        }

        private fun checkScheme(scheme: String?): ServerBaseUrlException? =
            if (scheme != HTTPS_SCHEME && scheme != HTTP_SCHEME) {
                ServerBaseUrlException("Server URL must start with https://.")
            } else {
                null
            }

        private fun checkHost(uri: URI): ServerBaseUrlException? =
            if (uri.host.isNullOrBlank()) ServerBaseUrlException("Server URL must include a host name.") else null

        private fun checkQueryOrFragment(uri: URI): ServerBaseUrlException? =
            if (uri.rawQuery != null || uri.rawFragment != null) {
                ServerBaseUrlException("Server URL cannot include query text or a fragment.")
            } else {
                null
            }

        private fun checkCleartextAllowed(scheme: String?, cleartextAllowed: Boolean): ServerBaseUrlException? =
            if (scheme == HTTP_SCHEME && !cleartextAllowed) {
                ServerBaseUrlException("Release builds require an HTTPS Simple Estimation server.")
            } else {
                null
            }

        private fun buildNormalized(scheme: String, uri: URI): ServerBaseUrl = ServerBaseUrl(
            URI(
                scheme,
                uri.userInfo,
                uri.host,
                uri.port,
                uri.path.takeUnless { it.isNullOrBlank() || it == "/" },
                null,
                null,
            ).toString().trimEnd('/'),
        )
    }
}

class ServerBaseUrlException(message: String) : IllegalArgumentException(message)
