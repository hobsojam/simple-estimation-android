package com.hobsojam.simpleestimation.feature.planningpoker

import com.hobsojam.simpleestimation.domain.room.Participant
import com.hobsojam.simpleestimation.domain.room.PokerItem
import com.hobsojam.simpleestimation.domain.room.PokerItemStatus
import com.hobsojam.simpleestimation.domain.room.PokerTimer
import com.hobsojam.simpleestimation.domain.room.SessionRoomState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PlanningPokerStateMapperTest :
    DescribeSpec({
        val baseRoom = SessionRoomState.PlanningPoker(
            id = "room-1",
            name = "Sprint Planning",
            pinProtected = false,
            facilitatorId = null,
            revealed = false,
            timer = PokerTimer(endsAt = null, durationSeconds = null, serverNow = 1_000_000L),
            participants = emptyList(),
            items = emptyList(),
        )

        describe("roomName") {
            it("uses room name when set") {
                val uiState = baseRoom.toUiState(displayName = "Alice", selectedVote = null)
                uiState.roomName shouldBe "Sprint Planning"
            }

            it("falls back to empty string when room name is null") {
                val uiState = baseRoom.copy(
                    name = null,
                ).toUiState(displayName = "Alice", selectedVote = null)
                uiState.roomName shouldBe ""
            }
        }

        describe("participantName") {
            it("uses the provided display name") {
                val uiState = baseRoom.toUiState(displayName = "Bob", selectedVote = null)
                uiState.participantName shouldBe "Bob"
            }
        }

        describe("activeBacklogItem") {
            it("returns the label of the active item") {
                val room = baseRoom.copy(
                    items = listOf(
                        PokerItem(
                            id = "i1",
                            label = "Feature A",
                            status = PokerItemStatus.Pending,
                            estimate = null,
                        ),
                        PokerItem(
                            id = "i2",
                            label = "Feature B",
                            status = PokerItemStatus.Active,
                            estimate = null,
                        ),
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.activeBacklogItem shouldBe "Feature B"
            }

            it("returns empty string when there is no active item") {
                val uiState = baseRoom.toUiState(displayName = "Alice", selectedVote = null)
                uiState.activeBacklogItem shouldBe ""
            }
        }

        describe("selectedVote") {
            it("passes through the provided selected vote") {
                val uiState = baseRoom.toUiState(displayName = "Alice", selectedVote = "8")
                uiState.selectedVote shouldBe "8"
            }

            it("passes through null when no vote is selected") {
                val uiState = baseRoom.toUiState(displayName = "Alice", selectedVote = null)
                uiState.selectedVote shouldBe null
            }
        }

        describe("votingProgress") {
            it("counts voted participants and total") {
                val room = baseRoom.copy(
                    participants = listOf(
                        Participant(id = "p1", name = "Alice", voted = true, vote = null),
                        Participant(id = "p2", name = "Bob", voted = false, vote = null),
                        Participant(id = "p3", name = "Carol", voted = true, vote = null),
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.votingProgress shouldBe VotingProgress(votedCount = 2, participantCount = 3)
            }

            it("returns zero progress when there are no participants") {
                val uiState = baseRoom.toUiState(displayName = "Alice", selectedVote = null)
                uiState.votingProgress shouldBe VotingProgress(votedCount = 0, participantCount = 0)
            }
        }

        describe("revealedVotes") {
            it("maps participant votes to revealed votes when revealed is true") {
                val room = baseRoom.copy(
                    revealed = true,
                    participants = listOf(
                        Participant(id = "p1", name = "Alice", voted = true, vote = "5"),
                        Participant(id = "p2", name = "Bob", voted = true, vote = "8"),
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.revealedVotes shouldBe listOf(
                    RevealedVote(participantName = "Alice", vote = "5"),
                    RevealedVote(participantName = "Bob", vote = "8"),
                )
            }

            it("excludes participants with null votes even when revealed") {
                val room = baseRoom.copy(
                    revealed = true,
                    participants = listOf(
                        Participant(id = "p1", name = "Alice", voted = true, vote = "5"),
                        Participant(id = "p2", name = "Bob", voted = false, vote = null),
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.revealedVotes shouldBe
                    listOf(RevealedVote(participantName = "Alice", vote = "5"))
            }

            it("returns empty list when votes are not revealed") {
                val room = baseRoom.copy(
                    revealed = false,
                    participants = listOf(
                        Participant(id = "p1", name = "Alice", voted = true, vote = null),
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.revealedVotes shouldBe emptyList()
            }
        }

        describe("outlierParticipantNames") {
            it("flags a vote more than double the median as an outlier") {
                val room = baseRoom.copy(
                    revealed = true,
                    participants = listOf(
                        Participant(id = "p1", name = "Alice", voted = true, vote = "5"),
                        Participant(id = "p2", name = "Bob", voted = true, vote = "5"),
                        Participant(id = "p3", name = "Riley", voted = true, vote = "13"),
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.outlierParticipantNames shouldBe setOf("Riley")
            }

            it("does not flag non-numeric votes as outliers") {
                val room = baseRoom.copy(
                    revealed = true,
                    participants = listOf(
                        Participant(id = "p1", name = "Alice", voted = true, vote = "5"),
                        Participant(id = "p2", name = "Bob", voted = true, vote = "?"),
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.outlierParticipantNames shouldBe emptySet()
            }

            it("returns empty set when fewer than two numeric votes exist") {
                val room = baseRoom.copy(
                    revealed = true,
                    participants = listOf(
                        Participant(id = "p1", name = "Alice", voted = true, vote = "5"),
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.outlierParticipantNames shouldBe emptySet()
            }
        }

        describe("timer") {
            it("returns null when endsAt is null") {
                val uiState = baseRoom.toUiState(displayName = "Alice", selectedVote = null)
                uiState.timer shouldBe null
            }

            it("computes remaining seconds from endsAt minus serverNow") {
                val room = baseRoom.copy(
                    timer = PokerTimer(
                        endsAt = 1_060_000L,
                        durationSeconds = 60L,
                        serverNow = 1_000_000L,
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.timer shouldBe PlanningPokerTimer(remainingSeconds = 60)
            }

            it("clamps remaining seconds to zero when the timer has expired") {
                val room = baseRoom.copy(
                    timer = PokerTimer(
                        endsAt = 900_000L,
                        durationSeconds = 60L,
                        serverNow = 1_000_000L,
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.timer shouldBe PlanningPokerTimer(remainingSeconds = 0)
            }
        }

        describe("acceptedEstimate") {
            it("returns null when votes are not revealed") {
                val uiState = baseRoom.toUiState(displayName = "Alice", selectedVote = null)
                uiState.acceptedEstimate shouldBe null
            }

            it("returns the estimate of the most recent done item when revealed") {
                val room = baseRoom.copy(
                    revealed = true,
                    items = listOf(
                        PokerItem(
                            id = "i1",
                            label = "Done item",
                            status = PokerItemStatus.Done,
                            estimate = "5",
                        ),
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.acceptedEstimate shouldBe "5"
            }

            it("returns null when revealed but no done items have an estimate") {
                val room = baseRoom.copy(
                    revealed = true,
                    items = listOf(
                        PokerItem(
                            id = "i1",
                            label = "Active item",
                            status = PokerItemStatus.Active,
                            estimate = null,
                        ),
                    ),
                )
                val uiState = room.toUiState(displayName = "Alice", selectedVote = null)
                uiState.acceptedEstimate shouldBe null
            }
        }
    })
