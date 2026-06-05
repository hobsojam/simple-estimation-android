package com.hobsojam.simpleestimation.feature.planningpoker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val SAMPLE_HIDDEN_VOTED_COUNT = 2
private const val SAMPLE_REVEALED_VOTED_COUNT = 3
private const val SAMPLE_PARTICIPANT_COUNT = 4
private const val SAMPLE_TIMER_SECONDS = 180

@Composable
fun PlanningPokerParticipantRoute() {
    var participantName by remember { mutableStateOf("") }
    var joinedName by remember { mutableStateOf<String?>(null) }
    var selectedVote by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val currentJoinedName = joinedName
        if (currentJoinedName == null) {
            JoinPlanningPokerScreen(
                participantName = participantName,
                onParticipantNameChange = { participantName = it },
                onJoin = { joinedName = participantName.trim() },
            )
        } else {
            PlanningPokerScreen(
                state = samplePlanningPokerState(
                    participantName = currentJoinedName,
                    selectedVote = selectedVote,
                ),
                onVoteSelected = { selectedVote = it },
            )
        }
    }
}

@Composable
fun PlanningPokerScreen(state: PlanningPokerUiState, onVoteSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoomHeader(state = state)
        ActiveBacklogItemCard(label = state.activeBacklogItem)
        VoteCards(
            selectedVote = state.selectedVote,
            onVoteSelected = onVoteSelected,
        )
        if (state.votesAreRevealed) {
            RevealedVotesCard(state = state)
        } else {
            VotingProgressCard(progress = state.votingProgress)
        }
        state.timer?.let { TimerCard(timer = it) }
    }
}

private fun samplePlanningPokerState(participantName: String, selectedVote: String?): PlanningPokerUiState {
    val votesAreRevealed = selectedVote != null
    val participantVote = selectedVote ?: "5"
    return PlanningPokerUiState(
        roomName = "Sprint planning",
        participantName = participantName,
        activeBacklogItem = "HOB-11 Build Planning Poker participant screen",
        selectedVote = selectedVote,
        votingProgress = VotingProgress(
            votedCount = if (selectedVote == null) {
                SAMPLE_HIDDEN_VOTED_COUNT
            } else {
                SAMPLE_REVEALED_VOTED_COUNT
            },
            participantCount = SAMPLE_PARTICIPANT_COUNT,
        ),
        timer = PlanningPokerTimer(remainingSeconds = SAMPLE_TIMER_SECONDS),
        revealedVotes = if (votesAreRevealed) {
            listOf(
                RevealedVote(participantName = participantName, vote = participantVote),
                RevealedVote(participantName = "Sam", vote = "5"),
                RevealedVote(participantName = "Riley", vote = "13"),
            )
        } else {
            emptyList()
        },
        outlierParticipantNames = if (votesAreRevealed) setOf("Riley") else emptySet(),
        acceptedEstimate = if (votesAreRevealed) "5" else null,
    )
}

@Preview(showBackground = true)
@Composable
private fun PlanningPokerParticipantRoutePreview() {
    MaterialTheme {
        PlanningPokerParticipantRoute()
    }
}
