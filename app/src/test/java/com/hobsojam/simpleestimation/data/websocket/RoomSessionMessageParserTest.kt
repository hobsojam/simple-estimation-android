package com.hobsojam.simpleestimation.data.websocket

import com.hobsojam.simpleestimation.domain.room.PokerItemStatus
import com.hobsojam.simpleestimation.domain.room.ServerErrorCode
import com.hobsojam.simpleestimation.domain.room.SessionError
import com.hobsojam.simpleestimation.domain.room.SessionRoomState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

class RoomSessionMessageParserTest :
    DescribeSpec({
        val parser = RoomSessionMessageParser()

        describe("state message — planning poker room") {
            it("parses a full planning poker state") {
                val result = parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "room-1",
                        "type": "planning-poker",
                        "name": "Sprint Planning",
                        "pinProtected": true,
                        "facilitatorId": "participant-1",
                        "revealed": false,
                        "timer": {
                          "endsAt": null,
                          "durationSeconds": null,
                          "serverNow": 1000000000
                        },
                        "participants": [
                          { "id": "participant-1", "name": "Alice", "voted": true, "vote": null },
                          { "id": "participant-2", "name": "Bob", "voted": false, "vote": null }
                        ],
                        "items": [
                          { "id": "item-1", "label": "Feature A", "status": "active", "estimate": null },
                          { "id": "item-2", "label": "Feature B", "status": "pending", "estimate": null },
                          { "id": "item-3", "label": "Feature C", "status": "done", "estimate": "5" }
                        ]
                      }
                    }
                    """.trimIndent(),
                ).getOrThrow()

                val msg = result.shouldBeInstanceOf<ParsedRoomMessage.RoomState>()
                val room = msg.state.shouldBeInstanceOf<SessionRoomState.PlanningPoker>()
                room.id shouldBe "room-1"
                room.name shouldBe "Sprint Planning"
                room.pinProtected shouldBe true
                room.facilitatorId shouldBe "participant-1"
                room.revealed shouldBe false
                room.timer.endsAt.shouldBeNull()
                room.timer.durationSeconds.shouldBeNull()
                room.timer.serverNow shouldBe 1000000000L
                room.participants shouldHaveSize 2
                room.participants[0].id shouldBe "participant-1"
                room.participants[0].name shouldBe "Alice"
                room.participants[0].voted shouldBe true
                room.participants[0].vote.shouldBeNull()
                room.participants[1].voted shouldBe false
                room.items shouldHaveSize 3
                room.items[0].id shouldBe "item-1"
                room.items[0].label shouldBe "Feature A"
                room.items[0].status shouldBe PokerItemStatus.Active
                room.items[0].estimate.shouldBeNull()
                room.items[1].status shouldBe PokerItemStatus.Pending
                room.items[2].status shouldBe PokerItemStatus.Done
                room.items[2].estimate shouldBe "5"
            }

            it("parses a running timer with non-null endsAt and durationSeconds") {
                val result = parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "room-1",
                        "type": "planning-poker",
                        "name": null,
                        "pinProtected": false,
                        "facilitatorId": null,
                        "revealed": false,
                        "timer": {
                          "endsAt": 1000000060,
                          "durationSeconds": 60,
                          "serverNow": 1000000000
                        },
                        "participants": [],
                        "items": []
                      }
                    }
                    """.trimIndent(),
                ).getOrThrow()

                val room = (result as ParsedRoomMessage.RoomState)
                    .state as SessionRoomState.PlanningPoker
                room.name.shouldBeNull()
                room.facilitatorId.shouldBeNull()
                room.timer.endsAt shouldBe 1000000060L
                room.timer.durationSeconds shouldBe 60L
                room.timer.serverNow shouldBe 1000000000L
            }

            it("parses a revealed state with visible votes") {
                val result = parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "room-1",
                        "type": "planning-poker",
                        "name": "Room",
                        "pinProtected": false,
                        "facilitatorId": null,
                        "revealed": true,
                        "timer": { "endsAt": null, "durationSeconds": null, "serverNow": 1 },
                        "participants": [
                          { "id": "p1", "name": "Alice", "voted": true, "vote": "5" }
                        ],
                        "items": []
                      }
                    }
                    """.trimIndent(),
                ).getOrThrow()

                val room = (result as ParsedRoomMessage.RoomState)
                    .state as SessionRoomState.PlanningPoker
                room.revealed shouldBe true
                room.participants[0].vote shouldBe "5"
            }

            it("ignores unknown fields in room, participant, and item objects") {
                val result = parser.parse(
                    """
                    {
                      "type": "state",
                      "extra": "ignored",
                      "room": {
                        "id": "room-1",
                        "type": "planning-poker",
                        "name": "Room",
                        "pinProtected": false,
                        "facilitatorId": null,
                        "revealed": false,
                        "timer": { "endsAt": null, "durationSeconds": null, "serverNow": 1, "extra": 42 },
                        "participants": [
                          { "id": "p1", "name": "Alice", "voted": false, "vote": null, "extra": true }
                        ],
                        "items": [
                          { "id": "i1", "label": "Item", "status": "pending", "estimate": null, "extra": [] }
                        ],
                        "unknown": { "nested": "value" }
                      }
                    }
                    """.trimIndent(),
                ).getOrThrow()

                result.shouldBeInstanceOf<ParsedRoomMessage.RoomState>()
                    .state.shouldBeInstanceOf<SessionRoomState.PlanningPoker>()
            }
        }

        describe("state message — bucket room") {
            it("parses a bucket room with null and non-null item positions") {
                val result = parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "room-b",
                        "type": "bucket",
                        "name": null,
                        "pinProtected": false,
                        "facilitatorId": "p1",
                        "revealed": false,
                        "participants": [],
                        "items": [
                          { "id": "i1", "label": "Task A", "position": null },
                          { "id": "i2", "label": "Task B", "position": "M" },
                          { "id": "i3", "label": "Task C", "position": "XL" }
                        ]
                      }
                    }
                    """.trimIndent(),
                ).getOrThrow()

                val room = (result as ParsedRoomMessage.RoomState)
                    .state as SessionRoomState.Bucket
                room.id shouldBe "room-b"
                room.facilitatorId shouldBe "p1"
                room.items shouldHaveSize 3
                room.items[0].position.shouldBeNull()
                room.items[1].position shouldBe "M"
                room.items[2].position shouldBe "XL"
            }

            it("parses all valid bucket positions") {
                for (position in listOf("XS", "S", "M", "L", "XL")) {
                    val result = parser.parse(
                        """
                        {
                          "type": "state",
                          "room": {
                            "id": "r",
                            "type": "bucket",
                            "name": null,
                            "pinProtected": false,
                            "facilitatorId": null,
                            "revealed": false,
                            "participants": [],
                            "items": [{ "id": "i", "label": "L", "position": "$position" }]
                          }
                        }
                        """.trimIndent(),
                    )
                    result.isSuccess shouldBe true
                }
            }
        }

        describe("state message — relative estimation room") {
            it("parses a relative room with positioned items") {
                val result = parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "room-r",
                        "type": "relative",
                        "name": "Relative Room",
                        "pinProtected": false,
                        "facilitatorId": null,
                        "revealed": false,
                        "participants": [],
                        "items": [
                          { "id": "i1", "label": "Task A", "position": null },
                          { "id": "i2", "label": "Task B", "position": "3" }
                        ]
                      }
                    }
                    """.trimIndent(),
                ).getOrThrow()

                val room = (result as ParsedRoomMessage.RoomState)
                    .state as SessionRoomState.Relative
                room.id shouldBe "room-r"
                room.items shouldHaveSize 2
                room.items[0].position.shouldBeNull()
                room.items[1].position shouldBe "3"
            }
        }

        describe("state message — access-protected redacted room") {
            it("parses a redacted state as AccessProtected") {
                val result = parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "room-protected",
                        "type": "planning-poker",
                        "name": "Protected Room",
                        "accessRequired": true,
                        "pinProtected": true,
                        "facilitatorId": null,
                        "revealed": false,
                        "participants": [],
                        "items": []
                      }
                    }
                    """.trimIndent(),
                ).getOrThrow()

                val msg = result.shouldBeInstanceOf<ParsedRoomMessage.RoomState>()
                val room = msg.state.shouldBeInstanceOf<SessionRoomState.AccessProtected>()
                room.id shouldBe "room-protected"
                room.name shouldBe "Protected Room"
                room.pinProtected shouldBe true
            }

            it("parses a redacted bucket room as AccessProtected") {
                val result = parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "room-b",
                        "type": "bucket",
                        "name": null,
                        "accessRequired": true,
                        "pinProtected": true,
                        "facilitatorId": null,
                        "revealed": false,
                        "participants": [],
                        "items": []
                      }
                    }
                    """.trimIndent(),
                ).getOrThrow()

                val room = (result as ParsedRoomMessage.RoomState)
                    .state as SessionRoomState.AccessProtected
                room.name.shouldBeNull()
            }
        }

        describe("error messages") {
            it("parses a known error code") {
                val result = parser.parse(
                    """{"type":"error","code":"access_pin_required","message":"Access PIN required"}""",
                ).getOrThrow()

                val msg = result.shouldBeInstanceOf<ParsedRoomMessage.ServerError>()
                val error = msg.error.shouldBeInstanceOf<SessionError.KnownError>()
                error.code shouldBe ServerErrorCode.AccessPinRequired
                error.userMessage.shouldNotBeNull()
                error.userMessage shouldBe "This room requires an access PIN."
            }

            it("parses admin_required error") {
                val result = parser.parse(
                    """{"type":"error","code":"admin_required","message":"Only the facilitator"}""",
                ).getOrThrow()

                val error = (result as ParsedRoomMessage.ServerError).error
                    as SessionError.KnownError
                error.code shouldBe ServerErrorCode.AdminRequired
                error.userMessage shouldBe "Only the facilitator can do that."
            }

            it("maps an unknown error code to UnknownError with a generic message") {
                val result = parser.parse(
                    """{"type":"error","code":"future_unknown_code","message":"Some future error"}""",
                ).getOrThrow()

                val msg = result.shouldBeInstanceOf<ParsedRoomMessage.ServerError>()
                val error = msg.error.shouldBeInstanceOf<SessionError.UnknownError>()
                error.userMessage.shouldNotBeNull()
            }

            it("does not use the server message string in the parsed error for known codes") {
                val serverMessage = "Only the facilitator can reveal votes"
                val result = parser.parse(
                    """{"type":"error","code":"admin_required","message":"$serverMessage"}""",
                ).getOrThrow()

                val error = (result as ParsedRoomMessage.ServerError).error
                    as SessionError.KnownError
                error.userMessage shouldNotContain serverMessage
            }

            it("maps all 30 known error codes to a non-blank user message") {
                for (code in ServerErrorCode.entries) {
                    val result = parser.parse(
                        """{"type":"error","code":"${code.protocolValue}","message":"msg"}""",
                    ).getOrThrow()
                    val error = (result as ParsedRoomMessage.ServerError).error
                        as SessionError.KnownError
                    error.code shouldBe code
                    error.userMessage.isNotBlank() shouldBe true
                }
            }
        }

        describe("malformed payloads") {
            it("returns failure for invalid JSON") {
                parser.parse("{not json}").isFailure shouldBe true
            }

            it("returns failure for an empty string") {
                parser.parse("").isFailure shouldBe true
            }

            it("returns failure when the type field is missing") {
                parser.parse("""{"room":{}}""").isFailure shouldBe true
            }

            it("returns failure for an unknown message type") {
                parser.parse("""{"type":"vote_cast","data":{}}""").isFailure shouldBe true
            }

            it("returns failure for a state message missing the room object") {
                parser.parse("""{"type":"state"}""").isFailure shouldBe true
            }

            it("returns failure for a state message with an unsupported room type") {
                parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "r", "type": "unknown-mode", "name": null,
                        "pinProtected": false, "facilitatorId": null, "revealed": false,
                        "participants": [], "items": []
                      }
                    }
                    """.trimIndent(),
                ).isFailure shouldBe true
            }

            it("returns failure when the room id is missing") {
                parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "type": "planning-poker", "name": null,
                        "pinProtected": false, "facilitatorId": null, "revealed": false,
                        "timer": {"endsAt":null,"durationSeconds":null,"serverNow":1},
                        "participants": [], "items": []
                      }
                    }
                    """.trimIndent(),
                ).isFailure shouldBe true
            }

            it("returns failure when the room id is empty after trimming") {
                parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "  ", "type": "planning-poker", "name": null,
                        "pinProtected": false, "facilitatorId": null, "revealed": false,
                        "timer": {"endsAt":null,"durationSeconds":null,"serverNow":1},
                        "participants": [], "items": []
                      }
                    }
                    """.trimIndent(),
                ).isFailure shouldBe true
            }

            it("returns failure when the room name exceeds the shared text limit") {
                val longName = "a".repeat(201)
                parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "r", "type": "planning-poker", "name": "$longName",
                        "pinProtected": false, "facilitatorId": null, "revealed": false,
                        "timer": {"endsAt":null,"durationSeconds":null,"serverNow":1},
                        "participants": [], "items": []
                      }
                    }
                    """.trimIndent(),
                ).isFailure shouldBe true
            }

            it("returns failure when serverNow is a floating-point number") {
                parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "r", "type": "planning-poker", "name": null,
                        "pinProtected": false, "facilitatorId": null, "revealed": false,
                        "timer": {"endsAt":null,"durationSeconds":null,"serverNow":1.5},
                        "participants": [], "items": []
                      }
                    }
                    """.trimIndent(),
                ).isFailure shouldBe true
            }

            it("returns failure when the timer is missing from a planning poker room") {
                parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "r", "type": "planning-poker", "name": null,
                        "pinProtected": false, "facilitatorId": null, "revealed": false,
                        "participants": [], "items": []
                      }
                    }
                    """.trimIndent(),
                ).isFailure shouldBe true
            }

            it("returns failure when a participant is missing the voted field") {
                parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "r", "type": "planning-poker", "name": null,
                        "pinProtected": false, "facilitatorId": null, "revealed": false,
                        "timer": {"endsAt":null,"durationSeconds":null,"serverNow":1},
                        "participants": [{ "id": "p1", "name": "Alice", "vote": null }],
                        "items": []
                      }
                    }
                    """.trimIndent(),
                ).isFailure shouldBe true
            }

            it("returns failure for an unknown planning poker item status") {
                parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "r", "type": "planning-poker", "name": null,
                        "pinProtected": false, "facilitatorId": null, "revealed": false,
                        "timer": {"endsAt":null,"durationSeconds":null,"serverNow":1},
                        "participants": [],
                        "items": [{ "id": "i1", "label": "Task", "status": "unknown", "estimate": null }]
                      }
                    }
                    """.trimIndent(),
                ).isFailure shouldBe true
            }

            it("returns failure for an unrecognized bucket item position") {
                parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "r", "type": "bucket", "name": null,
                        "pinProtected": false, "facilitatorId": null, "revealed": false,
                        "participants": [],
                        "items": [{ "id": "i1", "label": "Task", "position": "INVALID" }]
                      }
                    }
                    """.trimIndent(),
                ).isFailure shouldBe true
            }

            it("returns failure when the items array exceeds 200") {
                val items = (1..201).joinToString(",") { i ->
                    """{"id":"i$i","label":"Item $i","status":"pending","estimate":null}"""
                }
                parser.parse(
                    """
                    {
                      "type": "state",
                      "room": {
                        "id": "room-1", "type": "planning-poker", "name": null,
                        "pinProtected": false, "facilitatorId": null, "revealed": false,
                        "timer": {"endsAt": null, "durationSeconds": null, "serverNow": 1000000},
                        "participants": [], "items": [$items]
                      }
                    }
                    """.trimIndent(),
                ).isFailure shouldBe true
            }

            it("returns failure for an error message missing the code field") {
                val result = parser.parse("""{"type":"error","message":"Something went wrong"}""")
                result.isFailure shouldBe true
            }
        }
    })
