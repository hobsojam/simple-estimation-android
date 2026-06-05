package com.hobsojam.simpleestimation.data.server

import com.hobsojam.simpleestimation.domain.server.ServerConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ServerConfigJsonParserTest : FunSpec({
    test("parses config while ignoring unknown fields") {
        val config = ServerConfigJsonParser.parse(
            """
            {
              "demoMode": true,
              "protocolVersion": 1,
              "extraField": "ignored"
            }
            """.trimIndent(),
        ).getOrThrow()

        config shouldBe ServerConfig(demoMode = true, protocolVersion = 1)
    }

    test("rejects malformed config without crashing") {
        val result = ServerConfigJsonParser.parse("{\"demoMode\": true}")

        result.exceptionOrNull() shouldNotBe null
    }
})
