package com.hobsojam.simpleestimation.feature.serverconfig

import com.hobsojam.simpleestimation.data.server.ServerConfigClient
import com.hobsojam.simpleestimation.domain.server.ServerBaseUrl
import com.hobsojam.simpleestimation.domain.server.ServerConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking

class ServerConfigurationControllerTest :
    FunSpec({
        test("calls api config and surfaces demo mode before joining") {
            val configClient =
                FakeServerConfigClient(ServerConfig(demoMode = true, protocolVersion = 1))
            val controller = ServerConfigurationController(
                configClient = configClient,
                cleartextAllowed = true,
            )
            controller.onServerUrlChanged("http://10.0.2.2:3000")
            controller.onRoomIdChanged(" room-1 ")
            controller.onDisplayNameChanged(" Ada ")

            runBlocking { controller.checkCompatibilityBeforeJoin() }

            configClient.requestedConfigEndpoint shouldBe "http://10.0.2.2:3000/api/config"
            controller.uiState.normalizedServerUrl shouldBe "http://10.0.2.2:3000"
            controller.uiState.normalizedWebSocketUrl shouldBe "ws://10.0.2.2:3000/ws"
            controller.uiState.status shouldBe ServerConfigurationStatus.ReadyToJoin(
                demoMode = true,
                message = "Protocol version 1 is supported. Ready to join room room-1.",
            )
        }

        test("shows upgrade message for unsupported protocol versions") {
            val controller = ServerConfigurationController(
                configClient = FakeServerConfigClient(
                    ServerConfig(demoMode = false, protocolVersion = 2),
                ),
                cleartextAllowed = false,
            )
            controller.onServerUrlChanged("https://example.com")
            controller.onRoomIdChanged("room-1")
            controller.onDisplayNameChanged("Ada")

            runBlocking { controller.checkCompatibilityBeforeJoin() }

            controller.uiState.status shouldBe ServerConfigurationStatus.BlockedByUpgrade(
                message = "This server uses protocol version 2. " +
                    "Update Simple Estimation for Android to join rooms on this server.",
            )
        }

        test("blocks cleartext URLs when cleartext is not allowed") {
            val controller = ServerConfigurationController(
                configClient = FakeServerConfigClient(
                    ServerConfig(demoMode = false, protocolVersion = 1),
                ),
                cleartextAllowed = false,
            )
            controller.onServerUrlChanged("http://example.com")
            controller.onRoomIdChanged("room-1")
            controller.onDisplayNameChanged("Ada")

            runBlocking { controller.checkCompatibilityBeforeJoin() }

            controller.uiState.status shouldBe ServerConfigurationStatus.Error(
                "Release builds require an HTTPS Simple Estimation server.",
            )
        }
    })

private class FakeServerConfigClient(private val config: ServerConfig) : ServerConfigClient {
    var requestedConfigEndpoint: String? = null
        private set

    override suspend fun fetchConfig(baseUrl: ServerBaseUrl): Result<ServerConfig> {
        requestedConfigEndpoint = baseUrl.configEndpoint()
        return Result.success(config)
    }
}
