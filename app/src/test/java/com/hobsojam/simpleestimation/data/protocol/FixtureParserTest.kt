package com.hobsojam.simpleestimation.data.protocol

import com.hobsojam.simpleestimation.data.server.ServerConfigJsonParser
import com.hobsojam.simpleestimation.data.websocket.ParsedRoomMessage
import com.hobsojam.simpleestimation.data.websocket.RoomSessionMessageParser
import com.hobsojam.simpleestimation.domain.room.SessionRoomState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private fun fixture(name: String): String =
    object {}.javaClass.getResourceAsStream("/fixtures/$name")!!
        .bufferedReader()
        .readText()

class FixtureParserTest :
    FunSpec({
        val wsParser = RoomSessionMessageParser()
        val roomsParser = ActiveRoomsJsonParser()
        val configParser = ServerConfigJsonParser

        test("server_config.json parses as non-demo protocolVersion 1") {
            val config = configParser.parse(fixture("server_config.json")).getOrThrow()
            config.protocolVersion shouldBe 1
            config.demoMode shouldBe false
        }

        test("server_config_demo_mode.json parses as demo protocolVersion 1") {
            val config = configParser.parse(fixture("server_config_demo_mode.json")).getOrThrow()
            config.protocolVersion shouldBe 1
            config.demoMode shouldBe true
        }

        test("active_rooms.json parses three rooms of distinct types") {
            val rooms = roomsParser.parse(fixture("active_rooms.json")).getOrThrow()
            rooms.map { it.type.name } shouldBe listOf("PlanningPoker", "Bucket", "Relative")
        }

        test("ws_state_planning_poker.json parses as PlanningPoker state") {
            val msg = wsParser.parse(fixture("ws_state_planning_poker.json")).getOrThrow()
            val state = msg.shouldBeInstanceOf<ParsedRoomMessage.RoomState>()
                .state.shouldBeInstanceOf<SessionRoomState.PlanningPoker>()
            state.name shouldBe "Sprint planning"
            state.items.size shouldBe 3
        }

        test("ws_state_bucket.json parses as Bucket state") {
            val msg = wsParser.parse(fixture("ws_state_bucket.json")).getOrThrow()
            val state = msg.shouldBeInstanceOf<ParsedRoomMessage.RoomState>()
                .state.shouldBeInstanceOf<SessionRoomState.Bucket>()
            state.items.size shouldBe 3
            state.items.first { it.id == "item-2" }.position shouldBe "M"
        }

        test("ws_state_relative.json parses as Relative state") {
            val msg = wsParser.parse(fixture("ws_state_relative.json")).getOrThrow()
            val state = msg.shouldBeInstanceOf<ParsedRoomMessage.RoomState>()
                .state.shouldBeInstanceOf<SessionRoomState.Relative>()
            state.items.first { it.id == "item-1" }.position shouldBe "8"
        }

        test("ws_state_access_protected.json parses as AccessProtected state") {
            val msg = wsParser.parse(fixture("ws_state_access_protected.json")).getOrThrow()
            msg.shouldBeInstanceOf<ParsedRoomMessage.RoomState>()
                .state.shouldBeInstanceOf<SessionRoomState.AccessProtected>()
        }

        test("ws_error_invalid_vote.json parses as ServerError") {
            val msg = wsParser.parse(fixture("ws_error_invalid_vote.json")).getOrThrow()
            msg.shouldBeInstanceOf<ParsedRoomMessage.ServerError>()
        }

        test("ws_error_join_before_voting.json parses as ServerError") {
            val msg = wsParser.parse(fixture("ws_error_join_before_voting.json")).getOrThrow()
            msg.shouldBeInstanceOf<ParsedRoomMessage.ServerError>()
        }
    })
