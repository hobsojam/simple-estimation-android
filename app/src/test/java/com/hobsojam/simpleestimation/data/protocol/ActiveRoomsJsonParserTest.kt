package com.hobsojam.simpleestimation.data.protocol

import com.hobsojam.simpleestimation.domain.room.EstimationRoomType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ActiveRoomsJsonParserTest : FunSpec({
    val parser = ActiveRoomsJsonParser()

    test("parses active rooms and ignores unknown fields") {
        val rooms = parser.parse(
            """
            [
              {
                "id": " 0f48bd1c-b892-4b62-97ea-6fbe4df7198d ",
                "type": "planning-poker",
                "name": "Sprint planning",
                "participantCount": 3,
                "pinProtected": true,
                "accessPinProtected": false,
                "unexpected": { "nested": "value" }
              }
            ]
            """.trimIndent(),
        ).getOrThrow()

        rooms.shouldHaveSize(1)
        rooms.single().id shouldBe "0f48bd1c-b892-4b62-97ea-6fbe4df7198d"
        rooms.single().type shouldBe EstimationRoomType.PlanningPoker
        rooms.single().name shouldBe "Sprint planning"
        rooms.single().participantCount shouldBe 3
        rooms.single().pinProtected shouldBe true
        rooms.single().accessPinProtected shouldBe false
    }

    test("accepts a null room name") {
        val rooms = parser.parse(
            """
            [
              {
                "id": "room-1",
                "type": "bucket",
                "name": null,
                "participantCount": 0,
                "pinProtected": false,
                "accessPinProtected": true
              }
            ]
            """.trimIndent(),
        ).getOrThrow()

        rooms.single().name shouldBe null
        rooms.single().type shouldBe EstimationRoomType.Bucket
    }

    test("rejects unknown room types") {
        parser.parse(
            """
            [
              {
                "id": "room-1",
                "type": "admin-only",
                "name": "Room",
                "participantCount": 0,
                "pinProtected": false,
                "accessPinProtected": false
              }
            ]
            """.trimIndent(),
        ).isFailure shouldBe true
    }

    test("rejects participant counts outside the server limit") {
        parser.parse(
            """
            [
              {
                "id": "room-1",
                "type": "relative",
                "name": "Room",
                "participantCount": 101,
                "pinProtected": false,
                "accessPinProtected": false
              }
            ]
            """.trimIndent(),
        ).isFailure shouldBe true
    }

    test("rejects empty and oversized text fields") {
        val oversizedName = "x".repeat(201)

        parser.parse(
            """
            [
              {
                "id": "room-1",
                "type": "planning-poker",
                "name": "$oversizedName",
                "participantCount": 1,
                "pinProtected": false,
                "accessPinProtected": false
              }
            ]
            """.trimIndent(),
        ).isFailure shouldBe true

        parser.parse(
            """
            [
              {
                "id": " ",
                "type": "planning-poker",
                "name": "Room",
                "participantCount": 1,
                "pinProtected": false,
                "accessPinProtected": false
              }
            ]
            """.trimIndent(),
        ).isFailure shouldBe true
    }
})
