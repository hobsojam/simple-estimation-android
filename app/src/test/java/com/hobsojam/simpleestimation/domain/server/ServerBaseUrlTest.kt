package com.hobsojam.simpleestimation.domain.server

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ServerBaseUrlTest :
    FunSpec({
        test("normalizes HTTPS server URLs and derives WSS endpoints") {
            val baseUrl = ServerBaseUrl.parse(
                rawValue = " https://example.com/ ",
                cleartextAllowed = false,
            ).getOrThrow()

            baseUrl.value shouldBe "https://example.com"
            baseUrl.configEndpoint() shouldBe "https://example.com/api/config"
            baseUrl.webSocketEndpoint() shouldBe "wss://example.com/ws"
        }

        test("accepts HTTP URLs only when cleartext is allowed") {
            val baseUrl = ServerBaseUrl.parse(
                rawValue = "http://10.0.2.2:3000",
                cleartextAllowed = true,
            ).getOrThrow()

            baseUrl.value shouldBe "http://10.0.2.2:3000"
            baseUrl.webSocketEndpoint() shouldBe "ws://10.0.2.2:3000/ws"
        }

        test("rejects HTTP URLs when cleartext is not allowed") {
            val result = ServerBaseUrl.parse(
                rawValue = "http://example.com",
                cleartextAllowed = false,
            )

            result.exceptionOrNull()?.message shouldBe
                "Release builds require an HTTPS Simple Estimation server."
        }

        test("rejects URLs with query strings") {
            val result = ServerBaseUrl.parse(
                rawValue = "https://example.com?room=1",
                cleartextAllowed = false,
            )

            result.exceptionOrNull() shouldNotBe null
        }

        test("derives WSS session URL with room ID and participant ID query parameters") {
            val baseUrl = ServerBaseUrl.parse(
                rawValue = "https://example.com",
                cleartextAllowed = false,
            ).getOrThrow()

            baseUrl.webSocketSessionUrl(
                roomId = "room-1",
                participantId = "participant-1",
            ) shouldBe "wss://example.com/ws?roomId=room-1&participantId=participant-1"
        }

        test("derives WS session URL with query parameters when cleartext is allowed") {
            val baseUrl = ServerBaseUrl.parse(
                rawValue = "http://10.0.2.2:3000",
                cleartextAllowed = true,
            ).getOrThrow()

            baseUrl.webSocketSessionUrl(
                roomId = "room-1",
                participantId = "participant-1",
            ) shouldBe "ws://10.0.2.2:3000/ws?roomId=room-1&participantId=participant-1"
        }
    })
