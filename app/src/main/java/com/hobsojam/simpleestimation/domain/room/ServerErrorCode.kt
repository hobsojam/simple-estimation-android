package com.hobsojam.simpleestimation.domain.room

enum class ServerErrorCode(val protocolValue: String, internal val userMessage: String) {
    AccessPinRequired("access_pin_required", "This room requires an access PIN."),
    ActiveItemRequired("active_item_required", "No item is currently active."),
    AdminRequired("admin_required", "Only the facilitator can do that."),
    DoneItemSelectionForbidden(
        "done_item_selection_forbidden",
        "Completed items cannot be made active again.",
    ),
    EstimateRequired("estimate_required", "An estimate is required."),
    InternalServerError(
        "internal_server_error",
        "The server encountered an error. Please try again.",
    ),
    InvalidAccessPin("invalid_access_pin", "The access PIN is incorrect."),
    InvalidEstimate("invalid_estimate", "That estimate is not valid for this room."),
    InvalidJson("invalid_json", "A message could not be understood by the server."),
    InvalidPin("invalid_pin", "The facilitator PIN is incorrect."),
    InvalidVote("invalid_vote", "That is not a valid vote for this room."),
    ItemIdRequired("item_id_required", "An item ID is required."),
    ItemLabelRequired("item_label_required", "An item label is required."),
    ItemLabelTooLong("item_label_too_long", "The item label is too long."),
    ItemLimitReached("item_limit_reached", "The room has reached its item limit."),
    ItemNotFound("item_not_found", "The requested item was not found."),
    ItemPositionInvalid("item_position_invalid", "That position is not valid for this room type."),
    ItemPositionRoomTypeInvalid(
        "item_position_room_type_invalid",
        "Items cannot be moved in this room type.",
    ),
    JoinBeforeMovingItems("join_before_moving_items", "Join the room before moving items."),
    JoinBeforeVoting("join_before_voting", "Join the room before casting a vote."),
    NameRequired("name_required", "A name is required to join."),
    NameTooLong("name_too_long", "Your name is too long."),
    PendingItemRequired("pending_item_required", "Only pending items can be removed."),
    PinRequired("pin_required", "A facilitator PIN is required."),
    PositionRequired("position_required", "A position is required."),
    RoomHasNoPin("room_has_no_pin", "This room does not have a facilitator PIN."),
    TimerDurationInvalid("timer_duration_invalid", "The timer duration is not valid."),
    TimerRoomTypeRequired(
        "timer_room_type_required",
        "Timers are only available in Planning Poker rooms.",
    ),
    UnknownMessageType("unknown_message_type", "An unrecognized action was sent."),
    VoteRequired("vote_required", "A vote is required."),
    ;

    companion object {
        fun fromProtocolValue(value: String): ServerErrorCode? =
            entries.firstOrNull { it.protocolValue == value }
    }
}
