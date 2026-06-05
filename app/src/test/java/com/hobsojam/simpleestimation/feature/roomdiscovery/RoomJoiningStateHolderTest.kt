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
            initialServerUrl = "https://example.com/",
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

    test("validates server URLs before producing a join request") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            initialServerUrl = "not a url",
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error(
            "Enter a valid server URL.",
        )
    }

    test("rejects cleartext server URLs unless cleartext is allowed") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            initialServerUrl = "http://example.com",
            cleartextAllowed = false,
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error(
            "Release builds require an HTTPS Simple Estimation server.",
        )
    }

    test("allows cleartext server URLs when cleartext is allowed") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            initialServerUrl = "http://10.0.2.2:3000/",
            cleartextAllowed = true,
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.ReadyToConnect(
            RoomJoinRequest(
                serverBaseUrl = "http://10.0.2.2:3000",
                roomId = "room-99",
                displayName = "Ada",
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

    test("clears stale access PINs when opening a different room link") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            initialServerUrl = "https://example.com",
        )
        val protectedRoom = ActiveRoom(
            id = "protected-room",
            type = EstimationRoomType.PlanningPoker,
            name = "Protected room",
            participantCount = 2,
            pinProtected = false,
            accessPinProtected = true,
        )

        stateHolder.selectRoom(protectedRoom)
        stateHolder.updateAccessPin("secret")
        stateHolder.openRoomLink("https://rooms.example.com/?room=public-room")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.ReadyToConnect(
            RoomJoinRequest(
                serverBaseUrl = "https://rooms.example.com",
                roomId = "public-room",
                displayName = "Ada",
                accessPin = null,
            ),
        )
    }

    test("clears stale access PINs when manual room input changes") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            initialServerUrl = "https://example.com",
        )
        val protectedRoom = ActiveRoom(
            id = "protected-room",
            type = EstimationRoomType.PlanningPoker,
            name = "Protected room",
            participantCount = 2,
            pinProtected = false,
            accessPinProtected = true,
        )

        stateHolder.selectRoom(protectedRoom)
        stateHolder.updateAccessPin("secret")
        stateHolder.updateManualRoomInput("public-room")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.ReadyToConnect(
            RoomJoinRequest(
                serverBaseUrl = "https://example.com",
                roomId = "public-room",
                displayName = "Ada",
                accessPin = null,
            ),
        )
    }

    test("clears stale access PINs when server URL changes") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            initialServerUrl = "https://example.com",
        )
        val protectedRoom = ActiveRoom(
            id = "protected-room",
            type = EstimationRoomType.PlanningPoker,
            name = "Protected room",
            participantCount = 2,
            pinProtected = false,
            accessPinProtected = true,
        )

        stateHolder.selectRoom(protectedRoom)
        stateHolder.updateAccessPin("secret")
        stateHolder.updateServerUrl("https://other.example.com")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error(
            "Enter the room access PIN to continue.",
        )
        stateHolder.uiState.join.accessPin shouldBe ""
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
