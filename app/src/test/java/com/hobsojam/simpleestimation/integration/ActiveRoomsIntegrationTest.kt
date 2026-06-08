package com.hobsojam.simpleestimation.integration

import com.hobsojam.simpleestimation.data.room.HttpActiveRoomRepository
import com.hobsojam.simpleestimation.domain.room.ActiveRoomDiscoveryResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ActiveRoomsIntegrationTest :
    FunSpec({
        tags(Integration)

        val repository = HttpActiveRoomRepository()

        test("GET /api/rooms returns a successful result") {
            val result = repository.loadActiveRooms(IntegrationServer.baseUrl)
            result.shouldBeInstanceOf<ActiveRoomDiscoveryResult.Success>()
        }

        test("GET /api/rooms response is parseable and rooms have required fields") {
            val result = repository.loadActiveRooms(IntegrationServer.baseUrl)
            val success = result.shouldBeInstanceOf<ActiveRoomDiscoveryResult.Success>()
            success.rooms.forEach { room ->
                room.id.isNotBlank() shouldBe true
                (room.participantCount >= 0) shouldBe true
            }
        }
    })
