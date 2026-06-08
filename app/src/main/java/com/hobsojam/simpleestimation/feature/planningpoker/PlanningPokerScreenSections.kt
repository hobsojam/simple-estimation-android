package com.hobsojam.simpleestimation.feature.planningpoker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val SECONDS_PER_MINUTE = 60
private const val CLOCK_SECONDS_WIDTH = 2

@Composable
internal fun ServerErrorBanner(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
internal fun RoomHeader(state: PlanningPokerUiState, onLeave: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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
        TextButton(onClick = onLeave) {
            Text("Leave")
        }
    }
}

@Composable
internal fun ActiveBacklogItemCard(label: String) {
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
internal fun VoteCards(selectedVote: String?, onVoteSelected: (String) -> Unit) {
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
internal fun VotingProgressCard(progress: VotingProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Voting progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val progressLabel =
                "${progress.votedCount} of ${progress.participantCount} participants have voted"
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
internal fun RevealedVotesCard(state: PlanningPokerUiState) {
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
internal fun TimerCard(timer: PlanningPokerTimer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Text(
            text = "Timer: ${timer.remainingSeconds.secondsAsClock()}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(16.dp)
                .semantics {
                    contentDescription =
                        "Timer ${timer.remainingSeconds.secondsAsClock()} remaining"
                },
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
