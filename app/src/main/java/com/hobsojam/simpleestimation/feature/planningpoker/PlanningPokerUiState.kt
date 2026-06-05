package com.hobsojam.simpleestimation.feature.planningpoker

val PlanningPokerVoteCards = listOf("1", "2", "3", "5", "8", "13", "21", "?", "∞", "☕")

data class PlanningPokerUiState(
    val roomName: String,
    val participantName: String,
    val activeBacklogItem: String,
    val selectedVote: String? = null,
    val votingProgress: VotingProgress,
    val timer: PlanningPokerTimer? = null,
    val revealedVotes: List<RevealedVote> = emptyList(),
    val outlierParticipantNames: Set<String> = emptySet(),
    val acceptedEstimate: String? = null,
) {
    val votesAreRevealed: Boolean = revealedVotes.isNotEmpty()
}

data class VotingProgress(
    val votedCount: Int,
    val participantCount: Int,
) {
    init {
        require(votedCount >= 0) { "votedCount must be non-negative" }
        require(participantCount >= 0) { "participantCount must be non-negative" }
        require(votedCount <= participantCount) { "votedCount cannot exceed participantCount" }
    }
}

data class PlanningPokerTimer(
    val remainingSeconds: Int,
) {
    init {
        require(remainingSeconds >= 0) { "remainingSeconds must be non-negative" }
    }
}

data class RevealedVote(
    val participantName: String,
    val vote: String,
)
