package com.hobsojam.simpleestimation.data.room

import com.hobsojam.simpleestimation.BuildConfig
import com.hobsojam.simpleestimation.data.protocol.ActiveRoomsJsonParser
import com.hobsojam.simpleestimation.domain.room.ActiveRoomDiscoveryFailure
import com.hobsojam.simpleestimation.domain.room.ActiveRoomDiscoveryResult
import com.hobsojam.simpleestimation.domain.room.ActiveRoomRepository
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ACTIVE_ROOMS_PATH = "/api/rooms"
private const val CONNECT_TIMEOUT_MILLIS = 10_000
private const val READ_TIMEOUT_MILLIS = 10_000
private const val HTTP_OK = 200

class HttpActiveRoomRepository(private val parser: ActiveRoomsJsonParser = ActiveRoomsJsonParser()) :
    ActiveRoomRepository {
    override suspend fun loadActiveRooms(serverBaseUrl: String): ActiveRoomDiscoveryResult =
        withContext(Dispatchers.IO) {
            val roomsUri = validatedRoomsUri(serverBaseUrl)
                ?: return@withContext ActiveRoomDiscoveryResult.Failure(
                    ActiveRoomDiscoveryFailure.InvalidServerUrl,
                )

            if (!BuildConfig.DEBUG && roomsUri.scheme != "https") {
                return@withContext ActiveRoomDiscoveryResult.Failure(
                    ActiveRoomDiscoveryFailure.InsecureServerUrl,
                )
            }

            runCatching { fetchRooms(roomsUri) }
                .getOrElse { error -> error.toDiscoveryFailure() }
        }

    private fun fetchRooms(roomsUri: URI): ActiveRoomDiscoveryResult {
        val connection = roomsUri.toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        connection.setRequestProperty("Accept", "application/json")

        return connection.useSafely {
            if (responseCode != HTTP_OK) {
                return@useSafely ActiveRoomDiscoveryResult.Failure(
                    ActiveRoomDiscoveryFailure.ServerUnavailable,
                )
            }

            val body = inputStream.bufferedReader().use { it.readText() }
            parser.parse(body).fold(
                onSuccess = { rooms -> ActiveRoomDiscoveryResult.Success(rooms) },
                onFailure = {
                    ActiveRoomDiscoveryResult.Failure(
                        ActiveRoomDiscoveryFailure.MalformedResponse,
                    )
                },
            )
        }
    }

    private fun Throwable.toDiscoveryFailure(): ActiveRoomDiscoveryResult.Failure = ActiveRoomDiscoveryResult.Failure(
        when (this) {
            is IOException -> ActiveRoomDiscoveryFailure.NetworkUnavailable
            else -> ActiveRoomDiscoveryFailure.MalformedResponse
        },
    )

    private fun validatedRoomsUri(serverBaseUrl: String): URI? = serverBaseUrl.trim()
        .takeIf { it.isNotEmpty() }
        ?.let { runCatching { URI(it) }.getOrNull() }
        ?.takeIf { it.isValidServerUri() }
        ?.let { uri ->
            val normalizedPath = uri.path.orEmpty().trimEnd('/')
            URI(
                uri.scheme,
                uri.userInfo,
                uri.host,
                uri.port,
                normalizedPath + ACTIVE_ROOMS_PATH,
                null,
                null,
            )
        }

    private fun URI.isValidServerUri(): Boolean = isValidSchemeAndHost() && hasNoCredentialsOrParams()

    private fun URI.isValidSchemeAndHost(): Boolean =
        scheme in setOf("http", "https") && !host.isNullOrBlank() && userInfo == null

    private fun URI.hasNoCredentialsOrParams(): Boolean = query == null && fragment == null

    private inline fun <T> HttpURLConnection.useSafely(block: HttpURLConnection.() -> T): T = try {
        block()
    } finally {
        disconnect()
    }
}
