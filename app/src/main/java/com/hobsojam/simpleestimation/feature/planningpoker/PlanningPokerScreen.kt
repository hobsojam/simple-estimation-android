package com.hobsojam.simpleestimation.feature.planningpoker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val SECONDS_PER_MINUTE = 60
private const val CLOCK_SECONDS_WIDTH = 2
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
fun JoinPlanningPokerScreen(
    participantName: String,
    onParticipantNameChange: (String) -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Join Planning Poker",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Enter the display name you want other participants to see in this room.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        OutlinedTextField(
            value = participantName,
            onValueChange = onParticipantNameChange,
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .semantics { contentDescription = "Display name" },
        )
        Button(
            onClick = onJoin,
            enabled = participantName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Join room"
                },
        ) {
            Text("Join room")
        }
    }
}

@Composable
fun PlanningPokerScreen(
    state: PlanningPokerUiState,
    onVoteSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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

@Composable
private fun RoomHeader(state: PlanningPokerUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = state.roomName,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Joined as ${state.participantName}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ActiveBacklogItemCard(label: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Active backlog item",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoteCards(
    selectedVote: String?,
    onVoteSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Choose your estimate",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlanningPokerVoteCards.forEach { vote ->
                val selected = vote == selectedVote
                FilterChip(
                    selected = selected,
                    onClick = { onVoteSelected(vote) },
                    label = { Text(vote) },
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = voteContentDescription(vote)
                        stateDescription = if (selected) "Selected" else "Not selected"
                    },
                )
            }
        }
    }
}

@Composable
private fun VotingProgressCard(progress: VotingProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Voting progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val progressLabel = "${progress.votedCount} of ${progress.participantCount} participants have voted"
            Text(
                text = progressLabel,
                modifier = Modifier.padding(top = 8.dp),
            )
            LinearProgressIndicator(
                progress = {
                    if (progress.participantCount == 0) {
                        0f
                    } else {
                        progress.votedCount.toFloat() / progress.participantCount.toFloat()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .semantics { contentDescription = progressLabel },
            )
        }
    }
}

@Composable
private fun RevealedVotesCard(state: PlanningPokerUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Revealed votes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            state.revealedVotes.forEach { revealedVote ->
                val isOutlier = revealedVote.participantName in state.outlierParticipantNames
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(revealedVote.participantName)
                    Text(
                        text = if (isOutlier) {
                            "${revealedVote.vote} · Outlier"
                        } else {
                            revealedVote.vote
                        },
                        fontWeight = if (isOutlier) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
            state.acceptedEstimate?.let { estimate ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Accepted estimate: $estimate",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun TimerCard(timer: PlanningPokerTimer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Text(
            text = "Timer: ${timer.remainingSeconds.secondsAsClock()}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(16.dp)
                .semantics { contentDescription = "Timer ${timer.remainingSeconds.secondsAsClock()} remaining" },
        )
    }
}

private fun voteContentDescription(vote: String): String = when (vote) {
    "∞" -> "Vote infinity"
    "☕" -> "Vote coffee"
    "?" -> "Vote unknown"
    else -> "Vote $vote"
}

private fun Int.secondsAsClock(): String {
    val minutes = this / SECONDS_PER_MINUTE
    val seconds = this % SECONDS_PER_MINUTE
    return "$minutes:${seconds.toString().padStart(length = CLOCK_SECONDS_WIDTH, padChar = '0')}"
}

private fun samplePlanningPokerState(
    participantName: String,
    selectedVote: String?,
): PlanningPokerUiState {
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
