package com.hobsojam.simpleestimation.data.websocket

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RoomJoinMessageBuilderTest :
    FunSpec({
        test("builds join message with display name and no access PIN") {
            RoomJoinMessageBuilder.build(displayName = "Alice", accessPin = null) shouldBe
                """{"type":"join","name":"Alice"}"""
        }

        test("builds join message with display name and access PIN") {
            RoomJoinMessageBuilder.build(displayName = "Alice", accessPin = "secret") shouldBe
                """{"type":"join","name":"Alice","accessPin":"secret"}"""
        }

        test("escapes double quotes in display name") {
            RoomJoinMessageBuilder.build(
                displayName = """Alice "Dev" Smith""",
                accessPin = null,
            ) shouldBe
                """{"type":"join","name":"Alice \"Dev\" Smith"}"""
        }

        test("escapes backslashes in display name") {
            RoomJoinMessageBuilder.build(displayName = "Alice\\Bob", accessPin = null) shouldBe
                """{"type":"join","name":"Alice\\Bob"}"""
        }

        test("escapes double quotes in access PIN") {
            RoomJoinMessageBuilder.build(
                displayName = "Alice",
                accessPin = """pin"with"quotes""",
            ) shouldBe
                """{"type":"join","name":"Alice","accessPin":"pin\"with\"quotes"}"""
        }

        test("omits access PIN field entirely when access PIN is null") {
            val message = RoomJoinMessageBuilder.build(displayName = "Alice", accessPin = null)

            message.contains("accessPin") shouldBe false
        }
    })
