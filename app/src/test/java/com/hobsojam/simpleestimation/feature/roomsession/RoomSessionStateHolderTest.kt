package com.hobsojam.simpleestimation.feature.roomsession

import com.hobsojam.simpleestimation.domain.room.RoomSession
import com.hobsojam.simpleestimation.domain.room.RoomSessionClient
import com.hobsojam.simpleestimation.domain.room.RoomSessionListener
import com.hobsojam.simpleestimation.domain.room.ServerErrorCode
import com.hobsojam.simpleestimation.domain.room.SessionError
import com.hobsojam.simpleestimation.domain.room.SessionRoomState
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomJoinRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class RoomSessionStateHolderTest :
    DescribeSpec({
        val validRequest = RoomJoinRequest(
            serverBaseUrl = "https://example.com",
            roomId = "room-1",
            participantId = "participant-1",
            displayName = "Alice",
            accessPin = null,
        )

        describe("connect") {
            it("transitions to Connecting when connect is called") {
                val stateHolder = RoomSessionStateHolder(FakeRoomSessionClient())

                stateHolder.connect(validRequest)

                stateHolder.state shouldBe RoomSessionState.Connecting
            }

            it("derives a WSS session URL from the validated base URL and room parameters") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)

                stateHolder.connect(validRequest)

                client.lastConnectUrl shouldBe
                    "wss://example.com/ws?roomId=room-1&participantId=participant-1"
            }

            it("derives a WS session URL when the base URL uses HTTP") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)

                stateHolder.connect(validRequest.copy(serverBaseUrl = "http://10.0.2.2:3000"))

                client.lastConnectUrl shouldBe
                    "ws://10.0.2.2:3000/ws?roomId=room-1&participantId=participant-1"
            }

            it("sends a join message with the display name and no access PIN") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)

                stateHolder.connect(validRequest)

                client.lastJoinMessage shouldBe """{"type":"join","name":"Alice"}"""
            }

            it("includes the access PIN in the join message when present") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)

                stateHolder.connect(validRequest.copy(accessPin = "secret"))

                client.lastJoinMessage shouldBe
                    """{"type":"join","name":"Alice","accessPin":"secret"}"""
            }

            it("closes the previous session when a new connection replaces it") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)

                stateHolder.connect(validRequest)
                val firstSession = client.lastSession!!
                stateHolder.connect(validRequest)

                firstSession.closeCount shouldBe 1
            }
        }

        describe("stale callbacks from replaced sessions") {
            it("ignores onOpen from a replaced session") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)

                stateHolder.connect(validRequest)
                val staleListener = client.lastListener!!
                stateHolder.connect(validRequest)

                staleListener.onOpen()

                stateHolder.state shouldBe RoomSessionState.Connecting
            }

            it("ignores onClosing from a replaced session") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)

                stateHolder.connect(validRequest)
                val staleListener = client.lastListener!!
                stateHolder.connect(validRequest)

                staleListener.onClosing(code = 1000, reason = "")

                stateHolder.state shouldBe RoomSessionState.Connecting
            }

            it("does not clear activeSession when onClosing fires from a replaced session") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)

                stateHolder.connect(validRequest)
                val staleListener = client.lastListener!!
                stateHolder.connect(validRequest)
                val activeSession = client.lastSession!!

                staleListener.onClosing(code = 1000, reason = "")
                stateHolder.disconnect()

                activeSession.closeCount shouldBe 1
            }

            it("ignores onFailure from a replaced session") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)

                stateHolder.connect(validRequest)
                val staleListener = client.lastListener!!
                stateHolder.connect(validRequest)

                staleListener.onFailure(Exception("network error"))

                stateHolder.state shouldBe RoomSessionState.Connecting
            }
        }

        describe("disconnect") {
            it("transitions to Idle after disconnect") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)
                stateHolder.connect(validRequest)

                stateHolder.disconnect()

                stateHolder.state shouldBe RoomSessionState.Idle
            }

            it("closes the active session on disconnect") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)
                stateHolder.connect(validRequest)
                val session = client.lastSession!!

                stateHolder.disconnect()

                session.closeCount shouldBe 1
            }

            it("no-ops when there is no active session") {
                val stateHolder = RoomSessionStateHolder(FakeRoomSessionClient())

                stateHolder.disconnect()

                stateHolder.state shouldBe RoomSessionState.Idle
            }
        }

        describe("onMessage") {
            it("transitions to Active and sets roomState on the first state message") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)
                stateHolder.connect(validRequest)

                client.lastListener!!.onOpen()
                client.lastListener!!.onMessage(PLANNING_POKER_STATE_JSON)

                val active = stateHolder.state.shouldBeInstanceOf<RoomSessionState.Active>()
                active.roomState.shouldBeInstanceOf<SessionRoomState.PlanningPoker>()
                active.lastError shouldBe null
            }

            it("updates roomState and clears lastError on subsequent state messages") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)
                stateHolder.connect(validRequest)
                client.lastListener!!.onOpen()
                client.lastListener!!.onMessage(PLANNING_POKER_STATE_JSON)
                client.lastListener!!.onMessage(ERROR_JSON)

                client.lastListener!!.onMessage(PLANNING_POKER_STATE_JSON)

                val active = stateHolder.state.shouldBeInstanceOf<RoomSessionState.Active>()
                active.roomState.shouldBeInstanceOf<SessionRoomState.PlanningPoker>()
                active.lastError shouldBe null
            }

            it("sets lastError on an error message while Active") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)
                stateHolder.connect(validRequest)
                client.lastListener!!.onOpen()
                client.lastListener!!.onMessage(PLANNING_POKER_STATE_JSON)

                client.lastListener!!.onMessage(ERROR_JSON)

                val active = stateHolder.state.shouldBeInstanceOf<RoomSessionState.Active>()
                val error = active.lastError.shouldBeInstanceOf<SessionError.KnownError>()
                error.code shouldBe ServerErrorCode.AdminRequired
            }

            it("ignores error messages received before any state message") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)
                stateHolder.connect(validRequest)
                client.lastListener!!.onOpen()

                client.lastListener!!.onMessage(ERROR_JSON)

                stateHolder.state.shouldBeInstanceOf<RoomSessionState.Active>()
                    .roomState shouldBe null
            }

            it("ignores malformed messages without changing roomState") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)
                stateHolder.connect(validRequest)
                client.lastListener!!.onOpen()
                client.lastListener!!.onMessage(PLANNING_POKER_STATE_JSON)
                val roomState = (stateHolder.state as RoomSessionState.Active).roomState

                client.lastListener!!.onMessage("{bad json}")

                (stateHolder.state as RoomSessionState.Active).roomState shouldBe roomState
            }

            it("ignores onMessage from a replaced session") {
                val client = FakeRoomSessionClient()
                val stateHolder = RoomSessionStateHolder(client)

                stateHolder.connect(validRequest)
                val staleListener = client.lastListener!!
                staleListener.onOpen()
                stateHolder.connect(validRequest)

                staleListener.onMessage(PLANNING_POKER_STATE_JSON)

                stateHolder.state shouldBe RoomSessionState.Connecting
            }
        }
    })

private const val PLANNING_POKER_STATE_JSON = """
{
  "type": "state",
  "room": {
    "id": "room-1",
    "type": "planning-poker",
    "name": "Test Room",
    "pinProtected": false,
    "facilitatorId": null,
    "revealed": false,
    "timer": { "endsAt": null, "durationSeconds": null, "serverNow": 1000000 },
    "participants": [],
    "items": []
  }
}
"""

private const val ERROR_JSON =
    """{"type":"error","code":"admin_required","message":"Only the facilitator"}"""

private class FakeRoomSessionClient : RoomSessionClient {
    var lastConnectUrl: String? = null
    var lastJoinMessage: String? = null
    var lastSession: FakeRoomSession? = null
    var lastListener: RoomSessionListener? = null

    override fun connect(
        url: String,
        joinMessage: String,
        listener: RoomSessionListener,
    ): RoomSession {
        lastConnectUrl = url
        lastJoinMessage = joinMessage
        lastListener = listener
        return FakeRoomSession().also { lastSession = it }
    }
}

private class FakeRoomSession : RoomSession {
    var closeCount = 0

    override fun close() {
        closeCount++
    }
}
