package com.hobsojam.simpleestimation.feature.planningpoker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

private const val MOST_PARTICIPANTS_VOTED = 3
private const val ALL_PARTICIPANTS_VOTED = 4
private const val PARTICIPANT_COUNT = 4
private const val TIMER_SECONDS = 90

class PlanningPokerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun joinsPlanningPokerRoom() {
        composeRule.setContent {
            MaterialTheme {
                PlanningPokerParticipantRoute()
            }
        }

        composeRule.onNodeWithText("Join Planning Poker").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Display name").performTextInput("Avery")
        composeRule.onNodeWithContentDescription("Join room")
            .assertIsEnabled()
            .performClick()

        composeRule.onNodeWithText("Joined as Avery").assertIsDisplayed()
        composeRule.onNodeWithText(
            "HOB-11 Build Planning Poker participant screen",
        ).assertIsDisplayed()
    }

    @Test
    fun selectsValidVoteCardAndShowsVotingProgressWhileVotesAreHidden() {
        var selectedVote: String? by mutableStateOf(null)
        composeRule.setContent {
            MaterialTheme {
                PlanningPokerScreen(
                    state = hiddenVotesState(selectedVote = selectedVote),
                    onVoteSelected = { selectedVote = it },
                )
            }
        }

        composeRule.onNodeWithText("3 of 4 participants have voted").assertIsDisplayed()
        PlanningPokerVoteCards.forEach { vote ->
            composeRule.onNodeWithContentDescription(
                voteContentDescriptionForTest(vote),
            ).assertIsDisplayed()
        }

        composeRule.onNodeWithContentDescription("Vote infinity").performClick()

        composeRule
            .onNode(
                hasContentDescription("Vote infinity") and hasStateDescription("Selected"),
            ).assertIsDisplayed()
    }

    @Test
    fun displaysTimerRevealedVotesOutliersAndAcceptedEstimate() {
        composeRule.setContent {
            MaterialTheme {
                PlanningPokerScreen(
                    state = revealedVotesState(),
                    onVoteSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Timer: 1:30").assertIsDisplayed()
        composeRule.onNodeWithText("Revealed votes").assertIsDisplayed()
        composeRule.onNodeWithText("Avery").assertIsDisplayed()
        composeRule.onNodeWithText("13 · Outlier").assertIsDisplayed()
        composeRule.onNodeWithText("Accepted estimate: 8").assertIsDisplayed()
    }

    @Test
    fun doesNotExposeFacilitatorOnlyActions() {
        composeRule.setContent {
            MaterialTheme {
                PlanningPokerScreen(
                    state = revealedVotesState(),
                    onVoteSelected = {},
                )
            }
        }

        facilitatorOnlyActions.forEach { action ->
            composeRule.onAllNodesWithText(action).assertCountEquals(0)
        }
    }
}

private fun hiddenVotesState(selectedVote: String?) = PlanningPokerUiState(
    roomName = "Sprint planning",
    participantName = "Avery",
    activeBacklogItem = "Build participant screen",
    selectedVote = selectedVote,
    votingProgress = VotingProgress(
        votedCount = MOST_PARTICIPANTS_VOTED,
        participantCount = PARTICIPANT_COUNT,
    ),
    timer = PlanningPokerTimer(remainingSeconds = TIMER_SECONDS),
)

private fun revealedVotesState() = PlanningPokerUiState(
    roomName = "Sprint planning",
    participantName = "Avery",
    activeBacklogItem = "Build participant screen",
    selectedVote = "8",
    votingProgress = VotingProgress(
        votedCount = ALL_PARTICIPANTS_VOTED,
        participantCount = PARTICIPANT_COUNT,
    ),
    timer = PlanningPokerTimer(remainingSeconds = TIMER_SECONDS),
    revealedVotes = listOf(
        RevealedVote(participantName = "Avery", vote = "8"),
        RevealedVote(participantName = "Riley", vote = "13"),
    ),
    outlierParticipantNames = setOf("Riley"),
    acceptedEstimate = "8",
)

private fun voteContentDescriptionForTest(vote: String): String = when (vote) {
    "∞" -> "Vote infinity"
    "☕" -> "Vote coffee"
    "?" -> "Vote unknown"
    else -> "Vote $vote"
}

private val facilitatorOnlyActions = listOf(
    "Reveal votes",
    "Reset round",
    "Accept estimate",
    "Start timer",
    "Pause timer",
    "Finalize item",
)
