package com.hobsojam.simpleestimation.feature.roomdiscovery

import com.hobsojam.simpleestimation.domain.room.ActiveRoom
import com.hobsojam.simpleestimation.domain.room.ActiveRoomDiscoveryResult
import com.hobsojam.simpleestimation.domain.room.ActiveRoomRepository
import com.hobsojam.simpleestimation.domain.room.EstimationRoomType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RoomJoiningStateHolderTest : FunSpec({
    test("selects an active room and requires an access PIN when the room is protected") {
        val room = ActiveRoom(
            id = "room-1",
            type = EstimationRoomType.PlanningPoker,
            name = "Sprint planning",
            participantCount = 3,
            pinProtected = false,
            accessPinProtected = true,
        )
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            initialServerUrl = "https://example.com",
        )

        stateHolder.selectRoom(room)
        stateHolder.updateDisplayName(" Ada ")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error("Enter the room access PIN to continue.")
        stateHolder.updateAccessPin(" secret ")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.ReadyToConnect(
            RoomJoinRequest(
                serverBaseUrl = "https://example.com",
                roomId = "room-1",
                displayName = "Ada",
                accessPin = "secret",
            ),
        )
    }

    test("opens a room link and extracts the room id without exposing link details in errors") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            initialServerUrl = "https://example.com",
        )

        stateHolder.openRoomLink("https://rooms.example.com/?room=room-42")

        stateHolder.uiState.serverUrl shouldBe "https://rooms.example.com"
        stateHolder.uiState.join.mode shouldBe RoomJoinMode.JoiningRoom(
            roomIdInput = "room-42",
            roomName = null,
            accessPinRequired = false,
        )

        stateHolder.openRoomLink("https://example.com/?notRoom=secret-internal-id")

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error(
            "Enter a valid room link or room ID.",
        )
    }

    test("manual room id joining trims room id and display name") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            initialServerUrl = "https://example.com",
        )

        stateHolder.updateManualRoomInput(" room-99 ")
        stateHolder.updateDisplayName(" Grace Hopper ")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.ReadyToConnect(
            RoomJoinRequest(
                serverBaseUrl = "https://example.com",
                roomId = "room-99",
                displayName = "Grace Hopper",
                accessPin = null,
            ),
        )
    }

    test("requires a server URL before producing a join request") {
        val stateHolder = RoomDiscoveryStateHolder(repository = FakeJoiningActiveRoomRepository())

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error(
            "Enter a server URL before joining.",
        )
    }

    test("validates display names with user-safe messages") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            initialServerUrl = "https://example.com",
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error(
            "Enter a display name before joining.",
        )
    }
})

private class FakeJoiningActiveRoomRepository : ActiveRoomRepository {
    override suspend fun loadActiveRooms(serverBaseUrl: String): ActiveRoomDiscoveryResult =
        ActiveRoomDiscoveryResult.Success(emptyList())
}
