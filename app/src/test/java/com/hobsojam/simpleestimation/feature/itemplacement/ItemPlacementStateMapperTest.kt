package com.hobsojam.simpleestimation.feature.itemplacement

import com.hobsojam.simpleestimation.domain.room.Participant
import com.hobsojam.simpleestimation.domain.room.PositionedItem
import com.hobsojam.simpleestimation.domain.room.SessionRoomState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ItemPlacementStateMapperTest :
    DescribeSpec({
        val baseBucket = SessionRoomState.Bucket(
            id = "room-1",
            name = "Sprint Planning",
            pinProtected = false,
            facilitatorId = null,
            revealed = false,
            participants = emptyList(),
            items = emptyList(),
        )

        val baseRelative = SessionRoomState.Relative(
            id = "room-1",
            name = "Sprint Planning",
            pinProtected = false,
            facilitatorId = null,
            revealed = false,
            participants = emptyList(),
            items = emptyList(),
        )

        describe("Bucket room") {
            describe("positions") {
                it("uses bucket positions XS S M L XL") {
                    val uiState = baseBucket.toUiState(displayName = "Alice")
                    uiState.positions shouldBe listOf("XS", "S", "M", "L", "XL")
                }
            }

            describe("roomName") {
                it("uses the room name when set") {
                    val uiState = baseBucket.toUiState(displayName = "Alice")
                    uiState.roomName shouldBe "Sprint Planning"
                }

                it("falls back to empty string when name is null") {
                    val uiState = baseBucket.copy(name = null).toUiState(displayName = "Alice")
                    uiState.roomName shouldBe ""
                }
            }

            describe("participantName") {
                it("uses the provided display name") {
                    val uiState = baseBucket.toUiState(displayName = "Bob")
                    uiState.participantName shouldBe "Bob"
                }
            }

            describe("participantCount") {
                it("counts participants from the room state") {
                    val room = baseBucket.copy(
                        participants = listOf(
                            Participant(id = "p1", name = "Alice", voted = false, vote = null),
                            Participant(id = "p2", name = "Bob", voted = false, vote = null),
                        ),
                    )
                    val uiState = room.toUiState(displayName = "Alice")
                    uiState.participantCount shouldBe 2
                }
            }

            describe("unplacedItems") {
                it("includes items with null position") {
                    val room = baseBucket.copy(
                        items = listOf(
                            PositionedItem(id = "i1", label = "Feature A", position = null),
                            PositionedItem(id = "i2", label = "Feature B", position = "XS"),
                        ),
                    )
                    val uiState = room.toUiState(displayName = "Alice")
                    uiState.unplacedItems shouldBe
                        listOf(PlacementItem(id = "i1", label = "Feature A"))
                }

                it("is empty when all items are placed") {
                    val room = baseBucket.copy(
                        items = listOf(
                            PositionedItem(id = "i1", label = "Feature A", position = "M"),
                        ),
                    )
                    val uiState = room.toUiState(displayName = "Alice")
                    uiState.unplacedItems shouldBe emptyList()
                }
            }

            describe("placedItems") {
                it("groups items by position") {
                    val room = baseBucket.copy(
                        items = listOf(
                            PositionedItem(id = "i1", label = "Feature A", position = "XS"),
                            PositionedItem(id = "i2", label = "Feature B", position = "XS"),
                            PositionedItem(id = "i3", label = "Feature C", position = "L"),
                        ),
                    )
                    val uiState = room.toUiState(displayName = "Alice")
                    uiState.placedItems["XS"] shouldBe listOf(
                        PlacementItem(id = "i1", label = "Feature A"),
                        PlacementItem(id = "i2", label = "Feature B"),
                    )
                    uiState.placedItems["L"] shouldBe
                        listOf(PlacementItem(id = "i3", label = "Feature C"))
                }

                it("includes all bucket positions as keys even when empty") {
                    val uiState = baseBucket.toUiState(displayName = "Alice")
                    uiState.placedItems.keys shouldBe setOf("XS", "S", "M", "L", "XL")
                    uiState.placedItems.values.forEach { it shouldBe emptyList() }
                }
            }

            describe("selectedItemId") {
                it("passes through the provided selected item ID") {
                    val uiState = baseBucket.toUiState(displayName = "Alice", selectedItemId = "i1")
                    uiState.selectedItemId shouldBe "i1"
                }

                it("is null when not provided") {
                    val uiState = baseBucket.toUiState(displayName = "Alice")
                    uiState.selectedItemId shouldBe null
                }
            }

            describe("serverError") {
                it("passes through the provided error message") {
                    val uiState = baseBucket.toUiState(
                        displayName = "Alice",
                        serverError = "Join the room before moving items.",
                    )
                    uiState.serverError shouldBe "Join the room before moving items."
                }

                it("is null when not provided") {
                    val uiState = baseBucket.toUiState(displayName = "Alice")
                    uiState.serverError shouldBe null
                }
            }
        }

        describe("Relative room") {
            describe("positions") {
                it("uses Fibonacci positions 1 2 3 5 8 13 21") {
                    val uiState = baseRelative.toUiState(displayName = "Alice")
                    uiState.positions shouldBe listOf("1", "2", "3", "5", "8", "13", "21")
                }
            }

            describe("placedItems") {
                it("includes all relative positions as keys even when empty") {
                    val uiState = baseRelative.toUiState(displayName = "Alice")
                    uiState.placedItems.keys shouldBe setOf("1", "2", "3", "5", "8", "13", "21")
                }

                it("groups items by Fibonacci position") {
                    val room = baseRelative.copy(
                        items = listOf(
                            PositionedItem(id = "i1", label = "Feature A", position = "5"),
                            PositionedItem(id = "i2", label = "Feature B", position = "13"),
                        ),
                    )
                    val uiState = room.toUiState(displayName = "Alice")
                    uiState.placedItems["5"] shouldBe
                        listOf(PlacementItem(id = "i1", label = "Feature A"))
                    uiState.placedItems["13"] shouldBe
                        listOf(PlacementItem(id = "i2", label = "Feature B"))
                }
            }
        }
    })
