package com.hobsojam.simpleestimation.data.server

import com.hobsojam.simpleestimation.domain.server.ServerBaseUrl
import com.hobsojam.simpleestimation.domain.server.ServerConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ServerConfigClient {
    suspend fun fetchConfig(baseUrl: ServerBaseUrl): Result<ServerConfig>
}

class JavaNetServerConfigClient : ServerConfigClient {
    override suspend fun fetchConfig(baseUrl: ServerBaseUrl): Result<ServerConfig> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(baseUrl.configEndpoint()).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.requestMethod = GET_METHOD
            connection.setRequestProperty(ACCEPT_HEADER, JSON_MEDIA_TYPE)
            connection.useCaches = false

            try {
                val statusCode = connection.responseCode
                if (statusCode !in HTTP_SUCCESS_RANGE) {
                    throw ServerConfigNetworkException(
                        "Could not read server configuration. Check the server URL and try again.",
                    )
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                ServerConfigJsonParser.parse(body).getOrThrow()
            } catch (exception: IOException) {
                throw ServerConfigNetworkException(
                    "Could not reach the Simple Estimation server. Check the server URL and try again.",
                    exception,
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 5_000
        const val READ_TIMEOUT_MILLIS = 5_000
        const val GET_METHOD = "GET"
        const val ACCEPT_HEADER = "Accept"
        const val JSON_MEDIA_TYPE = "application/json"
        val HTTP_SUCCESS_RANGE = 200..299
    }
}

class ServerConfigNetworkException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)
