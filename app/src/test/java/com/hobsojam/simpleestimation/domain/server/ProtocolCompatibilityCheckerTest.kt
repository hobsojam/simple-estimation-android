package com.hobsojam.simpleestimation.domain.server

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ProtocolCompatibilityCheckerTest :
    FunSpec({
        test("accepts protocol version 1") {
            val compatibility = ProtocolCompatibilityChecker.check(
                ServerConfig(demoMode = false, protocolVersion = 1),
            )

            compatibility shouldBe ProtocolCompatibility.Compatible(
                ServerConfig(demoMode = false, protocolVersion = 1),
            )
        }

        test("blocks unsupported protocol versions with an upgrade message") {
            val compatibility = ProtocolCompatibilityChecker.check(
                ServerConfig(demoMode = false, protocolVersion = 2),
            )

            compatibility shouldBe ProtocolCompatibility.Unsupported(
                protocolVersion = 2,
                message = "This server uses protocol version 2. " +
                    "Update Simple Estimation for Android to join rooms on this server.",
            )
        }
    })
