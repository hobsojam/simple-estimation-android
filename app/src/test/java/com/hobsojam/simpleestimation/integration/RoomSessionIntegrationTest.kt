package com.hobsojam.simpleestimation.integration

import com.hobsojam.simpleestimation.data.websocket.OkHttpRoomSessionClient
import com.hobsojam.simpleestimation.data.websocket.ParsedRoomMessage
import com.hobsojam.simpleestimation.data.websocket.RoomJoinMessageBuilder
import com.hobsojam.simpleestimation.data.websocket.RoomSessionMessageParser
import com.hobsojam.simpleestimation.data.websocket.VoteMessageBuilder
import com.hobsojam.simpleestimation.domain.room.RoomSessionListener
import com.hobsojam.simpleestimation.domain.room.SessionRoomState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class RoomSessionIntegrationTest :
    FunSpec({
        tags(Integration)

        val sessionClient = OkHttpRoomSessionClient()
        val parser = RoomSessionMessageParser()

        fun createRoom(type: String = "planning-poker"): String {
            val url = URI("${IntegrationServer.baseUrl}/api/rooms").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            try {
                val body = """{"type":"$type"}"""
                connection.outputStream.use { it.write(body.toByteArray()) }
                check(connection.responseCode == 200) {
                    "Room creation failed with HTTP ${connection.responseCode}"
                }
                val response = connection.inputStream.bufferedReader().readText()
                val idMatch = Regex(""""id"\s*:\s*"([^"]+)"""").find(response)
                return checkNotNull(idMatch?.groupValues?.get(1)) {
                    "Could not extract room ID from response: $response"
                }
            } finally {
                connection.disconnect()
            }
        }

        fun deleteRoom(roomId: String) {
            val url = URI("${IntegrationServer.baseUrl}/api/rooms/$roomId").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            try {
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        }

        test("WebSocket join receives an initial state message") {
            val roomId = createRoom("planning-poker")
            try {
                val participantId = UUID.randomUUID().toString()
                val latch = CountDownLatch(1)
                val received = AtomicReference<String?>()

                val wsUrl = "${IntegrationServer.wsBaseUrl}/ws?roomId=$roomId" +
                    "&participantId=$participantId"
                val joinMessage = RoomJoinMessageBuilder.build(
                    displayName = "Integration Tester",
                    accessPin = null,
                )

                val session = sessionClient.connect(
                    url = wsUrl,
                    joinMessage = joinMessage,
                    listener = object : RoomSessionListener {
                        override fun onOpen() {}
                        override fun onMessage(text: String) {
                            received.compareAndSet(null, text)
                            latch.countDown()
                        }
                        override fun onClosing(code: Int, reason: String) {}
                        override fun onFailure(cause: Throwable) {
                            latch.countDown()
                        }
                    },
                )

                latch.await(10, TimeUnit.SECONDS) shouldBe true
                val text = checkNotNull(received.get()) { "No message received" }
                val msg = parser.parse(text).getOrThrow()
                val state = msg.shouldBeInstanceOf<ParsedRoomMessage.RoomState>()
                state.state.shouldBeInstanceOf<SessionRoomState.PlanningPoker>()

                session.close()
            } finally {
                deleteRoom(roomId)
            }
        }

        test("voting sends a vote and the server echoes an updated state") {
            val roomId = createRoom("planning-poker")
            try {
                val participantId = UUID.randomUUID().toString()
                val readyLatch = CountDownLatch(1)
                val votedLatch = CountDownLatch(1)
                val votedState = AtomicReference<SessionRoomState.PlanningPoker?>()

                val wsUrl = "${IntegrationServer.wsBaseUrl}/ws?roomId=$roomId" +
                    "&participantId=$participantId"
                val joinMessage = RoomJoinMessageBuilder.build(
                    displayName = "Voter",
                    accessPin = null,
                )

                val session = sessionClient.connect(
                    url = wsUrl,
                    joinMessage = joinMessage,
                    listener = object : RoomSessionListener {
                        override fun onOpen() {}
                        override fun onMessage(text: String) {
                            val parsed = parser.parse(text).getOrNull()
                            if (parsed is ParsedRoomMessage.RoomState) {
                                readyLatch.countDown()
                                val poker = parsed.state as? SessionRoomState.PlanningPoker
                                    ?: return
                                if (poker.participants.any { it.id == participantId && it.voted }) {
                                    votedState.set(poker)
                                    votedLatch.countDown()
                                }
                            }
                        }
                        override fun onClosing(code: Int, reason: String) {}
                        override fun onFailure(cause: Throwable) {
                            readyLatch.countDown()
                            votedLatch.countDown()
                        }
                    },
                )

                readyLatch.await(10, TimeUnit.SECONDS) shouldBe true
                session.send(VoteMessageBuilder.build("5"))

                votedLatch.await(10, TimeUnit.SECONDS) shouldBe true
                val state = checkNotNull(votedState.get()) { "No post-vote state received" }
                val participant = state.participants.find { it.id == participantId }
                checkNotNull(participant) { "Participant not found in state" }
                participant.voted shouldBe true

                session.close()
            } finally {
                deleteRoom(roomId)
            }
        }

        test("sending an invalid vote returns an error message") {
            val roomId = createRoom("planning-poker")
            try {
                val participantId = UUID.randomUUID().toString()
                val stateLatch = CountDownLatch(1)
                val errorLatch = CountDownLatch(1)
                val errorMsg = AtomicReference<String?>()

                val wsUrl = "${IntegrationServer.wsBaseUrl}/ws?roomId=$roomId" +
                    "&participantId=$participantId"
                val joinMessage = RoomJoinMessageBuilder.build(
                    displayName = "Bad Voter",
                    accessPin = null,
                )

                val session = sessionClient.connect(
                    url = wsUrl,
                    joinMessage = joinMessage,
                    listener = object : RoomSessionListener {
                        override fun onOpen() {}
                        override fun onMessage(text: String) {
                            val parsed = parser.parse(text).getOrNull()
                            when (parsed) {
                                is ParsedRoomMessage.RoomState -> stateLatch.countDown()
                                is ParsedRoomMessage.ServerError -> {
                                    errorMsg.set(parsed.error.userMessage)
                                    errorLatch.countDown()
                                }
                                null -> {}
                            }
                        }
                        override fun onClosing(code: Int, reason: String) {}
                        override fun onFailure(cause: Throwable) {
                            stateLatch.countDown()
                            errorLatch.countDown()
                        }
                    },
                )

                stateLatch.await(10, TimeUnit.SECONDS)
                session.send("""{"type":"vote","vote":"99"}""")
                errorLatch.await(10, TimeUnit.SECONDS) shouldBe true
                checkNotNull(errorMsg.get()) { "No error received" }.isNotBlank() shouldBe true

                session.close()
            } finally {
                deleteRoom(roomId)
            }
        }
    })
