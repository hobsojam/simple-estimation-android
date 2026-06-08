package com.hobsojam.simpleestimation.feature.planningpoker

import com.hobsojam.simpleestimation.domain.room.PokerItemStatus
import com.hobsojam.simpleestimation.domain.room.PokerTimer
import com.hobsojam.simpleestimation.domain.room.SessionRoomState

internal fun SessionRoomState.PlanningPoker.toUiState(
    displayName: String,
    selectedVote: String?,
): PlanningPokerUiState {
    val activeItem = items.firstOrNull { it.status == PokerItemStatus.Active }
    val revealedVotes = if (revealed) {
        participants.mapNotNull { participant ->
            participant.vote?.let { vote ->
                RevealedVote(participantName = participant.name, vote = vote)
            }
        }
    } else {
        emptyList()
    }
    return PlanningPokerUiState(
        roomName = name ?: "",
        participantName = displayName,
        activeBacklogItem = activeItem?.label ?: "",
        selectedVote = selectedVote,
        votingProgress = VotingProgress(
            votedCount = participants.count { it.voted },
            participantCount = participants.size,
        ),
        timer = computeRemainingTimer(timer),
        revealedVotes = revealedVotes,
        outlierParticipantNames = computeOutliers(revealedVotes),
        acceptedEstimate = if (revealed) {
            activeItem?.estimate ?: items.lastOrNull { it.status == PokerItemStatus.Done }?.estimate
        } else {
            null
        },
    )
}

private fun computeRemainingTimer(timer: PokerTimer): PlanningPokerTimer? {
    val endsAt = timer.endsAt ?: return null
    val remainingMs = endsAt - timer.serverNow
    return PlanningPokerTimer(remainingSeconds = (remainingMs / 1000L).coerceAtLeast(0L).toInt())
}

private fun computeOutliers(votes: List<RevealedVote>): Set<String> {
    val numericVotes = votes.mapNotNull { it.vote.toIntOrNull() }
    if (numericVotes.size < 2) return emptySet()
    val sorted = numericVotes.sorted()
    val median = if (sorted.size % 2 == 0) {
        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
    } else {
        sorted[sorted.size / 2].toDouble()
    }
    return if (median == 0.0) {
        emptySet()
    } else {
        votes
            .filter { vote ->
                val numeric = vote.vote.toIntOrNull() ?: return@filter false
                numeric > median * 2 || numeric < median / 2
            }
            .map { it.participantName }
            .toSet()
    }
}
