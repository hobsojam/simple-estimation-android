package com.hobsojam.simpleestimation.feature.roomdiscovery

import com.hobsojam.simpleestimation.domain.room.ActiveRoom
import com.hobsojam.simpleestimation.domain.room.ActiveRoomDiscoveryFailure
import com.hobsojam.simpleestimation.domain.room.ActiveRoomDiscoveryResult
import com.hobsojam.simpleestimation.domain.room.ActiveRoomRepository
import com.hobsojam.simpleestimation.domain.room.EstimationRoomType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RoomDiscoveryStateHolderTest : FunSpec({
    val fixedClock = Clock.fixed(Instant.parse("2026-06-05T12:00:00Z"), ZoneOffset.UTC)

    test("loads active rooms from the configured server") {
        val room = ActiveRoom(
            id = "room-1",
            type = EstimationRoomType.PlanningPoker,
            name = "Sprint planning",
            participantCount = 2,
            pinProtected = true,
            accessPinProtected = false,
        )
        val repository = FakeActiveRoomRepository(ActiveRoomDiscoveryResult.Success(listOf(room)))
        val stateHolder = RoomDiscoveryStateHolder(
            repository = repository,
            clock = fixedClock,
            initialServerUrl = "https://example.com",
        )

        stateHolder.loadActiveRooms()

        repository.requestedUrls shouldBe listOf("https://example.com")
        val status = stateHolder.uiState.status as RoomDiscoveryStatus.Loaded
        status.rooms shouldBe listOf(room)
        status.loadedAt shouldBe Instant.parse("2026-06-05T12:00:00Z")
        status.isStale shouldBe false
    }

    test("represents an empty active-room response clearly") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeActiveRoomRepository(ActiveRoomDiscoveryResult.Success(emptyList())),
            clock = fixedClock,
            initialServerUrl = "https://example.com",
        )

        stateHolder.loadActiveRooms()

        stateHolder.uiState.status shouldBe RoomDiscoveryStatus.Empty(
            loadedAt = Instant.parse("2026-06-05T12:00:00Z"),
        )
    }

    test("keeps previous rooms as stale data when refresh fails") {
        val room = ActiveRoom(
            id = "room-1",
            type = EstimationRoomType.Bucket,
            name = null,
            participantCount = 1,
            pinProtected = false,
            accessPinProtected = true,
        )
        val repository = FakeActiveRoomRepository(
            ActiveRoomDiscoveryResult.Success(listOf(room)),
            ActiveRoomDiscoveryResult.Failure(ActiveRoomDiscoveryFailure.NetworkUnavailable),
        )
        val stateHolder = RoomDiscoveryStateHolder(
            repository = repository,
            clock = fixedClock,
            initialServerUrl = "https://example.com",
        )

        stateHolder.loadActiveRooms()
        stateHolder.loadActiveRooms()

        val status = stateHolder.uiState.status as RoomDiscoveryStatus.Error
        status.message shouldBe "Could not reach the server. Check the URL and connection."
        status.staleRooms.shouldHaveSize(1)
        status.staleRooms shouldBe listOf(room)
    }
})

private class FakeActiveRoomRepository(
    private vararg val results: ActiveRoomDiscoveryResult,
) : ActiveRoomRepository {
    val requestedUrls = mutableListOf<String>()
    private var requestCount = 0

    override suspend fun loadActiveRooms(serverBaseUrl: String): ActiveRoomDiscoveryResult {
        requestedUrls += serverBaseUrl
        return results[requestCount++]
    }
}
