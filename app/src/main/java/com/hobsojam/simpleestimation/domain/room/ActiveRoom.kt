package com.hobsojam.simpleestimation.domain.room

data class ActiveRoom(
    val id: String,
    val type: EstimationRoomType,
    val name: String?,
    val participantCount: Int,
    val pinProtected: Boolean,
    val accessPinProtected: Boolean,
)
