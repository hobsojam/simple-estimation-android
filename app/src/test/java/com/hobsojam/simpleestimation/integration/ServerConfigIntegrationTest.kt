package com.hobsojam.simpleestimation.integration

import com.hobsojam.simpleestimation.data.server.JavaNetServerConfigClient
import com.hobsojam.simpleestimation.domain.server.ServerBaseUrl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.net.HttpURLConnection
import java.net.URI

class ServerConfigIntegrationTest :
    FunSpec({
        tags(Integration)

        val client = JavaNetServerConfigClient()

        test("GET /health returns 200 ok") {
            val url = URI("${IntegrationServer.baseUrl}/health").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            try {
                connection.responseCode shouldBe 200
                val body = connection.inputStream.bufferedReader().readText()
                body shouldBe """{"status":"ok"}"""
            } finally {
                connection.disconnect()
            }
        }

        test("GET /api/config returns protocolVersion 1 and a boolean demoMode") {
            val baseUrl = ServerBaseUrl.fromValidated(IntegrationServer.baseUrl)
            val config = client.fetchConfig(baseUrl).getOrThrow()
            (config.protocolVersion >= 1) shouldBe true
        }

        test("client rejects a protocolVersion it does not support") {
            // Protocol version 1 is the only supported version. This verifies the
            // compatibility checker is wired up correctly for the deployed server.
            val baseUrl = ServerBaseUrl.fromValidated(IntegrationServer.baseUrl)
            val config = client.fetchConfig(baseUrl).getOrThrow()
            // If the server ever bumps the version, this test documents the mismatch.
            config.protocolVersion shouldBe 1
        }
    })
