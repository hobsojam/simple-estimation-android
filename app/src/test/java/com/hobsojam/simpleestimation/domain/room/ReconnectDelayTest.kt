package com.hobsojam.simpleestimation.domain.room

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ReconnectDelayTest :
    FunSpec({
        test("attempt 1 returns 1000 ms") {
            reconnectDelayMs(1) shouldBe 1_000L
        }

        test("attempt 2 returns 2000 ms") {
            reconnectDelayMs(2) shouldBe 2_000L
        }

        test("attempt 3 returns 4000 ms") {
            reconnectDelayMs(3) shouldBe 4_000L
        }

        test("attempt 4 returns 8000 ms") {
            reconnectDelayMs(4) shouldBe 8_000L
        }

        test("attempt 5 returns 16000 ms") {
            reconnectDelayMs(5) shouldBe 16_000L
        }

        test("attempt 6 is capped at 30000 ms") {
            reconnectDelayMs(6) shouldBe 30_000L
        }

        test("large attempt numbers are capped at 30000 ms") {
            reconnectDelayMs(100) shouldBe 30_000L
        }
    })
