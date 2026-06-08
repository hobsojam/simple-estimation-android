package com.hobsojam.simpleestimation.data.protocol

import com.hobsojam.simpleestimation.domain.room.ActiveRoom
import com.hobsojam.simpleestimation.domain.room.EstimationRoomType

private const val MAX_PARTICIPANTS = 100

private fun JsonValue.ObjectValue.requiredBoundedInt(name: String, range: IntRange): Int =
    requiredInt(name).takeIf { it in range } ?: throw ProtocolParseException()

class ActiveRoomsJsonParser {
    fun parse(json: String): Result<List<ActiveRoom>> = runCatching {
        val value = JsonParser(json).parse()
        val rooms = value as? JsonValue.ArrayValue ?: throw ProtocolParseException()
        rooms.values.map { roomValue -> roomValue.toActiveRoom() }
    }

    private fun JsonValue.toActiveRoom(): ActiveRoom {
        val room = this as? JsonValue.ObjectValue ?: throw ProtocolParseException()
        val id = room.requiredString("id").trim().requireSharedText()
        val type = EstimationRoomType.fromProtocolValue(room.requiredString("type"))
            ?: throw ProtocolParseException()
        val name = room.optionalString("name")?.trim()?.requireSharedText()
        val participantCount = room.requiredBoundedInt("participantCount", 0..MAX_PARTICIPANTS)

        return ActiveRoom(
            id = id,
            type = type,
            name = name,
            participantCount = participantCount,
            pinProtected = room.requiredBoolean("pinProtected"),
            accessPinProtected = room.requiredBoolean("accessPinProtected"),
        )
    }
}
