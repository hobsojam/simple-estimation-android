package com.hobsojam.simpleestimation.domain.room

enum class EstimationRoomType(val protocolValue: String, val displayName: String) {
    PlanningPoker(protocolValue = "planning-poker", displayName = "Planning Poker"),
    Bucket(protocolValue = "bucket", displayName = "Bucket Estimation"),
    Relative(protocolValue = "relative", displayName = "Relative Estimation"),
    ;

    companion object {
        fun fromProtocolValue(value: String): EstimationRoomType? = entries.firstOrNull { it.protocolValue == value }
    }
}
