package com.hobsojam.simpleestimation.domain.room

sealed interface SessionRoomState {
    data class PlanningPoker(
        val id: String,
        val name: String?,
        val pinProtected: Boolean,
        val facilitatorId: String?,
        val revealed: Boolean,
        val timer: PokerTimer,
        val participants: List<Participant>,
        val items: List<PokerItem>,
    ) : SessionRoomState

    data class Bucket(
        val id: String,
        val name: String?,
        val pinProtected: Boolean,
        val facilitatorId: String?,
        val revealed: Boolean,
        val participants: List<Participant>,
        val items: List<PositionedItem>,
    ) : SessionRoomState

    data class Relative(
        val id: String,
        val name: String?,
        val pinProtected: Boolean,
        val facilitatorId: String?,
        val revealed: Boolean,
        val participants: List<Participant>,
        val items: List<PositionedItem>,
    ) : SessionRoomState

    data class AccessProtected(val id: String, val name: String?, val pinProtected: Boolean) :
        SessionRoomState
}

data class Participant(val id: String, val name: String, val voted: Boolean, val vote: String?)

data class PokerTimer(val endsAt: Long?, val durationSeconds: Long?, val serverNow: Long)

enum class PokerItemStatus {
    Pending,
    Active,
    Done,
    ;

    companion object {
        fun fromProtocolValue(value: String): PokerItemStatus? = when (value) {
            "pending" -> Pending
            "active" -> Active
            "done" -> Done
            else -> null
        }
    }
}

data class PokerItem(
    val id: String,
    val label: String,
    val status: PokerItemStatus,
    val estimate: String?,
)

data class PositionedItem(val id: String, val label: String, val position: String?)
