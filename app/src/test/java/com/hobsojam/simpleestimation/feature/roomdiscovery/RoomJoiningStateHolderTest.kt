package com.hobsojam.simpleestimation.feature.roomdiscovery

import com.hobsojam.simpleestimation.domain.room.ActiveRoom
import com.hobsojam.simpleestimation.domain.room.ActiveRoomDiscoveryResult
import com.hobsojam.simpleestimation.domain.room.ActiveRoomRepository
import com.hobsojam.simpleestimation.domain.room.EstimationRoomType
import com.hobsojam.simpleestimation.data.server.ServerConfigClient
import com.hobsojam.simpleestimation.data.server.ServerConfigNetworkException
import com.hobsojam.simpleestimation.data.server.ServerConfigParseException
import com.hobsojam.simpleestimation.domain.server.ServerBaseUrl
import com.hobsojam.simpleestimation.domain.server.ServerConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            configClient = compatibleConfigClient(),
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-1"),
        )

        stateHolder.selectRoom(room)
        stateHolder.updateDisplayName(" Ada ")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error("Enter the room access PIN to continue.")
        stateHolder.updateAccessPin(" secret ")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.ReadyToConnect(
            request = RoomJoinRequest(
                serverBaseUrl = "https://example.com",
                roomId = "room-1",
                participantId = "participant-1",
                displayName = "Ada",
                accessPin = "secret",
            ),
            demoMode = false,
        )
    }

    test("opens a room link and extracts the room id without exposing link details in errors") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = compatibleConfigClient(),
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
            configClient = compatibleConfigClient(),
            initialServerUrl = "https://example.com/",
            participantIdGenerator = ParticipantIdSequence("participant-1"),
        )

        stateHolder.updateManualRoomInput(" room-99 ")
        stateHolder.updateDisplayName(" Grace Hopper ")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.ReadyToConnect(
            request = RoomJoinRequest(
                serverBaseUrl = "https://example.com",
                roomId = "room-99",
                participantId = "participant-1",
                displayName = "Grace Hopper",
                accessPin = null,
            ),
            demoMode = false,
        )
    }

    test("validates server URLs before producing a join request") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = compatibleConfigClient(),
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
            configClient = compatibleConfigClient(),
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
            configClient = compatibleConfigClient(),
            initialServerUrl = "http://10.0.2.2:3000/",
            cleartextAllowed = true,
            participantIdGenerator = ParticipantIdSequence("participant-1"),
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.ReadyToConnect(
            request = RoomJoinRequest(
                serverBaseUrl = "http://10.0.2.2:3000",
                roomId = "room-99",
                participantId = "participant-1",
                displayName = "Ada",
                accessPin = null,
            ),
            demoMode = false,
        )
    }

    test("requires a server URL before producing a join request") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = compatibleConfigClient(),
        )

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
            configClient = compatibleConfigClient(),
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-1"),
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
            request = RoomJoinRequest(
                serverBaseUrl = "https://rooms.example.com",
                roomId = "public-room",
                participantId = "participant-1",
                displayName = "Ada",
                accessPin = null,
            ),
            demoMode = false,
        )
    }

    test("clears stale access PINs when manual room input changes") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = compatibleConfigClient(),
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-1"),
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
            request = RoomJoinRequest(
                serverBaseUrl = "https://example.com",
                roomId = "public-room",
                participantId = "participant-1",
                displayName = "Ada",
                accessPin = null,
            ),
            demoMode = false,
        )
    }

    test("clears stale access PINs when server URL changes") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = compatibleConfigClient(),
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
            configClient = compatibleConfigClient(),
            initialServerUrl = "https://example.com",
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error(
            "Enter a display name before joining.",
        )
    }

    test("reuses participant id for repeated joins to the same room in the current session") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = compatibleConfigClient(),
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-1", "participant-2"),
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()
        val firstRequest = (stateHolder.uiState.join.status as RoomJoinStatus.ReadyToConnect).request

        stateHolder.submitJoin()
        val secondRequest = (stateHolder.uiState.join.status as RoomJoinStatus.ReadyToConnect).request

        firstRequest.participantId shouldBe "participant-1"
        secondRequest.participantId shouldBe "participant-1"
    }

    test("uses separate participant ids for separate rooms in the current session") {
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = compatibleConfigClient(),
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-1", "participant-2"),
        )

        stateHolder.updateDisplayName("Ada")
        stateHolder.updateManualRoomInput("room-1")
        stateHolder.submitJoin()
        val firstRoomRequest = (stateHolder.uiState.join.status as RoomJoinStatus.ReadyToConnect).request

        stateHolder.updateManualRoomInput("room-2")
        stateHolder.submitJoin()
        val secondRoomRequest = (stateHolder.uiState.join.status as RoomJoinStatus.ReadyToConnect).request

        firstRoomRequest.participantId shouldBe "participant-1"
        secondRoomRequest.participantId shouldBe "participant-2"
    }

    test("resets participant ids when a new app session state holder is created") {
        val firstSession = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = compatibleConfigClient(),
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-1"),
        )
        val secondSession = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = compatibleConfigClient(),
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-2"),
        )

        firstSession.updateManualRoomInput("room-99")
        firstSession.updateDisplayName("Ada")
        firstSession.submitJoin()
        secondSession.updateManualRoomInput("room-99")
        secondSession.updateDisplayName("Ada")
        secondSession.submitJoin()

        val firstRequest = (firstSession.uiState.join.status as RoomJoinStatus.ReadyToConnect).request
        val secondRequest = (secondSession.uiState.join.status as RoomJoinStatus.ReadyToConnect).request
        firstRequest.participantId shouldBe "participant-1"
        secondRequest.participantId shouldBe "participant-2"
    }

    test("keeps display name only in the current app session and does not carry access pins forward") {
        val currentSession = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = compatibleConfigClient(),
            initialServerUrl = "https://example.com",
        )

        currentSession.updateDisplayName("Ada")
        currentSession.updateAccessPin("secret")
        currentSession.cancelJoin()

        currentSession.uiState.join.displayName shouldBe "Ada"
        currentSession.uiState.join.accessPin shouldBe ""

        val nextSession = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = compatibleConfigClient(),
            initialServerUrl = "https://example.com",
        )

        nextSession.uiState.join.displayName shouldBe ""
        nextSession.uiState.join.accessPin shouldBe ""
    }

    test("checks compatible server config before a manual room join connects") {
        val configClient = FakeServerConfigClient(
            listOf(Result.success(ServerConfig(demoMode = false, protocolVersion = 1))),
        )
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = configClient,
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-1"),
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        configClient.requestedUrls.map { it.value } shouldBe listOf("https://example.com")
        stateHolder.uiState.join.status shouldBe RoomJoinStatus.ReadyToConnect(
            request = RoomJoinRequest(
                serverBaseUrl = "https://example.com",
                roomId = "room-99",
                participantId = "participant-1",
                displayName = "Ada",
                accessPin = null,
            ),
            demoMode = false,
        )
    }

    test("checks compatible server config before a selected active room join connects") {
        val configClient = FakeServerConfigClient(
            listOf(Result.success(ServerConfig(demoMode = false, protocolVersion = 1))),
        )
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = configClient,
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-1"),
        )

        stateHolder.selectRoom(
            ActiveRoom(
                id = "room-1",
                type = EstimationRoomType.PlanningPoker,
                name = "Sprint planning",
                participantCount = 3,
                pinProtected = false,
                accessPinProtected = false,
            ),
        )
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        configClient.requestedUrls.map { it.value } shouldBe listOf("https://example.com")
        val status = stateHolder.uiState.join.status as RoomJoinStatus.ReadyToConnect
        status.request.roomId shouldBe "room-1"
    }

    test("checks compatible server config before a room link join connects") {
        val configClient = FakeServerConfigClient(
            listOf(Result.success(ServerConfig(demoMode = false, protocolVersion = 1))),
        )
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = configClient,
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-1"),
        )

        stateHolder.openRoomLink("https://rooms.example.com/?room=room-42")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        configClient.requestedUrls.map { it.value } shouldBe listOf("https://rooms.example.com")
        val status = stateHolder.uiState.join.status as RoomJoinStatus.ReadyToConnect
        status.request.serverBaseUrl shouldBe "https://rooms.example.com"
        status.request.roomId shouldBe "room-42"
    }

    test("blocks unsupported protocol versions and preserves retry input") {
        val configClient = FakeServerConfigClient(
            listOf(Result.success(ServerConfig(demoMode = false, protocolVersion = 2))),
        )
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = configClient,
            initialServerUrl = "https://example.com",
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.updateAccessPin("secret")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error(
            "This server uses protocol version 2. Update Simple Estimation for Android to join rooms on this server.",
        )
        stateHolder.uiState.serverUrl shouldBe "https://example.com"
        stateHolder.uiState.join.displayName shouldBe "Ada"
        stateHolder.uiState.join.accessPin shouldBe "secret"
        stateHolder.uiState.join.mode shouldBe RoomJoinMode.JoiningRoom(
            roomIdInput = "room-99",
            roomName = null,
            accessPinRequired = false,
        )
    }

    test("surfaces demo mode before connecting") {
        val configClient = FakeServerConfigClient(
            listOf(Result.success(ServerConfig(demoMode = true, protocolVersion = 1))),
        )
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = configClient,
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-1"),
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        val status = stateHolder.uiState.join.status as RoomJoinStatus.ReadyToConnect
        status.demoMode shouldBe true
    }

    test("reports network failures from config checks without clearing retry input") {
        val configClient = FakeServerConfigClient(
            listOf(
                Result.failure(
                    ServerConfigNetworkException(
                        "Could not reach the Simple Estimation server. Check the server URL and try again.",
                    ),
                ),
            ),
        )
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = configClient,
            initialServerUrl = "https://example.com",
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error(
            "Could not reach the Simple Estimation server. Check the server URL and try again.",
        )
        (stateHolder.uiState.join.mode as RoomJoinMode.JoiningRoom).roomIdInput shouldBe "room-99"
        stateHolder.uiState.join.displayName shouldBe "Ada"
    }

    test("reports malformed config responses without connecting") {
        val configClient = FakeServerConfigClient(
            listOf(Result.failure(ServerConfigParseException("missing protocol"))),
        )
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = configClient,
            initialServerUrl = "https://example.com",
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error(
            "The server returned an unsupported configuration. Update Simple Estimation or try another server.",
        )
    }

    test("does not fetch config when release cleartext validation fails") {
        val configClient = FakeServerConfigClient(
            listOf(Result.success(ServerConfig(demoMode = false, protocolVersion = 1))),
        )
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = configClient,
            initialServerUrl = "http://example.com",
            cleartextAllowed = false,
        )

        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")
        stateHolder.submitJoin()

        configClient.requestedUrls shouldBe emptyList()
        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Error(
            "Release builds require an HTTPS Simple Estimation server.",
        )
    }

    test("ignores stale config response when cancel is called while in flight") {
        val deferred = CompletableDeferred<Result<ServerConfig>>()
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = ControllableServerConfigClient(deferred),
            initialServerUrl = "https://example.com",
        )
        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")

        val job = launch(Dispatchers.Unconfined) { stateHolder.submitJoin() }
        stateHolder.cancelJoin()
        deferred.complete(Result.success(ServerConfig(demoMode = false, protocolVersion = 1)))
        job.join()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Idle
    }

    test("ignores stale config response when server url changes while in flight") {
        val deferred = CompletableDeferred<Result<ServerConfig>>()
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = ControllableServerConfigClient(deferred),
            initialServerUrl = "https://example.com",
        )
        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")

        val job = launch(Dispatchers.Unconfined) { stateHolder.submitJoin() }
        stateHolder.updateServerUrl("https://other.example.com")
        deferred.complete(Result.success(ServerConfig(demoMode = false, protocolVersion = 1)))
        job.join()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.Idle
    }

    test("ignores first stale config response when join is submitted again while in flight") {
        val firstDeferred = CompletableDeferred<Result<ServerConfig>>()
        val secondResult = Result.success(ServerConfig(demoMode = false, protocolVersion = 1))
        val stateHolder = RoomDiscoveryStateHolder(
            repository = FakeJoiningActiveRoomRepository(),
            configClient = ControllableServerConfigClient(firstDeferred, listOf(secondResult)),
            initialServerUrl = "https://example.com",
            participantIdGenerator = ParticipantIdSequence("participant-1"),
        )
        stateHolder.updateManualRoomInput("room-99")
        stateHolder.updateDisplayName("Ada")

        val firstJob = launch(Dispatchers.Unconfined) { stateHolder.submitJoin() }
        val secondJob = launch(Dispatchers.Unconfined) { stateHolder.submitJoin() }
        firstDeferred.complete(Result.success(ServerConfig(demoMode = false, protocolVersion = 1)))
        firstJob.join()
        secondJob.join()

        stateHolder.uiState.join.status shouldBe RoomJoinStatus.ReadyToConnect(
            request = RoomJoinRequest(
                serverBaseUrl = "https://example.com",
                roomId = "room-99",
                participantId = "participant-1",
                displayName = "Ada",
                accessPin = null,
            ),
            demoMode = false,
        )
    }

})

private fun compatibleConfigClient(): ServerConfigClient = FakeServerConfigClient(
    listOf(
        Result.success(ServerConfig(demoMode = false, protocolVersion = 1)),
        Result.success(ServerConfig(demoMode = false, protocolVersion = 1)),
        Result.success(ServerConfig(demoMode = false, protocolVersion = 1)),
    ),
)

private class FakeJoiningActiveRoomRepository : ActiveRoomRepository {
    override suspend fun loadActiveRooms(serverBaseUrl: String): ActiveRoomDiscoveryResult =
        ActiveRoomDiscoveryResult.Success(emptyList())
}

private class ParticipantIdSequence(
    private vararg val values: String,
) : () -> String {
    private var nextIndex = 0

    override fun invoke(): String = values[nextIndex++]
}


private class FakeServerConfigClient(
    private val results: List<Result<ServerConfig>>,
) : ServerConfigClient {
    val requestedUrls = mutableListOf<ServerBaseUrl>()
    private var requestCount = 0

    override suspend fun fetchConfig(baseUrl: ServerBaseUrl): Result<ServerConfig> {
        requestedUrls += baseUrl
        return results[requestCount++]
    }
}

private class ControllableServerConfigClient(
    private val firstDeferred: CompletableDeferred<Result<ServerConfig>>,
    private val subsequentResults: List<Result<ServerConfig>> = emptyList(),
) : ServerConfigClient {
    private var callCount = 0

    override suspend fun fetchConfig(baseUrl: ServerBaseUrl): Result<ServerConfig> =
        if (callCount++ == 0) firstDeferred.await() else subsequentResults[callCount - 2]
}
