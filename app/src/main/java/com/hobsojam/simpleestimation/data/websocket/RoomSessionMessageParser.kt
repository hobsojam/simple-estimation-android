package com.hobsojam.simpleestimation.data.websocket

import com.hobsojam.simpleestimation.data.protocol.JsonParser
import com.hobsojam.simpleestimation.data.protocol.JsonValue
import com.hobsojam.simpleestimation.data.protocol.ProtocolParseException
import com.hobsojam.simpleestimation.data.protocol.optionalBoolean
import com.hobsojam.simpleestimation.data.protocol.optionalLong
import com.hobsojam.simpleestimation.data.protocol.optionalString
import com.hobsojam.simpleestimation.data.protocol.requireSharedText
import com.hobsojam.simpleestimation.data.protocol.requiredArray
import com.hobsojam.simpleestimation.data.protocol.requiredBoolean
import com.hobsojam.simpleestimation.data.protocol.requiredLong
import com.hobsojam.simpleestimation.data.protocol.requiredObject
import com.hobsojam.simpleestimation.data.protocol.requiredString
import com.hobsojam.simpleestimation.domain.room.Participant
import com.hobsojam.simpleestimation.domain.room.PokerItem
import com.hobsojam.simpleestimation.domain.room.PokerItemStatus
import com.hobsojam.simpleestimation.domain.room.PokerTimer
import com.hobsojam.simpleestimation.domain.room.PositionedItem
import com.hobsojam.simpleestimation.domain.room.ServerErrorCode
import com.hobsojam.simpleestimation.domain.room.SessionError
import com.hobsojam.simpleestimation.domain.room.SessionRoomState

private const val MAX_PARTICIPANTS = 100
private const val MAX_ITEMS = 500

private val VALID_BUCKET_POSITIONS = setOf("XS", "S", "M", "L", "XL")
private const val UNKNOWN_ERROR_MESSAGE = "The server reported an error. Please try again."

sealed interface ParsedRoomMessage {
    data class RoomState(val state: SessionRoomState) : ParsedRoomMessage
    data class ServerError(val error: SessionError) : ParsedRoomMessage
}

internal class RoomSessionMessageParser {
    fun parse(text: String): Result<ParsedRoomMessage> = runCatching {
        val root = JsonParser(text).parse() as? JsonValue.ObjectValue
            ?: throw ProtocolParseException()
        when (root.requiredString("type")) {
            "state" -> ParsedRoomMessage.RoomState(parseRoomState(root))
            "error" -> ParsedRoomMessage.ServerError(parseServerError(root))
            else -> throw ProtocolParseException()
        }
    }

    private fun parseRoomState(root: JsonValue.ObjectValue): SessionRoomState {
        val room = root.requiredObject("room")
        val id = room.requiredString("id").trim().requireSharedText()
        val name = room.optionalString("name")?.trim()?.requireSharedText()
        val pinProtected = room.requiredBoolean("pinProtected")
        val accessRequired = room.optionalBoolean("accessRequired") ?: false

        if (accessRequired) {
            return SessionRoomState.AccessProtected(
                id = id,
                name = name,
                pinProtected = pinProtected,
            )
        }

        val facilitatorId = room.optionalString("facilitatorId")?.trim()?.requireSharedText()
        val revealed = room.requiredBoolean("revealed")
        val rawParticipants = room.requiredArray("participants")
        rawParticipants.requireSizeAtMost(MAX_PARTICIPANTS)
        val participants = rawParticipants.map { parseParticipant(it) }
        val rawItems = room.requiredArray("items")
        rawItems.requireSizeAtMost(MAX_ITEMS)

        return when (room.requiredString("type")) {
            "planning-poker" -> SessionRoomState.PlanningPoker(
                id = id,
                name = name,
                pinProtected = pinProtected,
                facilitatorId = facilitatorId,
                revealed = revealed,
                timer = parsePokerTimer(room.requiredObject("timer")),
                participants = participants,
                items = rawItems.map { parsePokerItem(it) },
            )
            "bucket" -> SessionRoomState.Bucket(
                id = id,
                name = name,
                pinProtected = pinProtected,
                facilitatorId = facilitatorId,
                revealed = revealed,
                participants = participants,
                items = rawItems.map { parseBucketItem(it) },
            )
            "relative" -> SessionRoomState.Relative(
                id = id,
                name = name,
                pinProtected = pinProtected,
                facilitatorId = facilitatorId,
                revealed = revealed,
                participants = participants,
                items = rawItems.map { parseRelativeItem(it) },
            )
            else -> throw ProtocolParseException()
        }
    }

    private fun <T> List<T>.requireSizeAtMost(max: Int) {
        if (size > max) throw ProtocolParseException()
    }

    private fun parsePokerTimer(obj: JsonValue.ObjectValue): PokerTimer = PokerTimer(
        endsAt = obj.optionalLong("endsAt"),
        durationSeconds = obj.optionalLong("durationSeconds"),
        serverNow = obj.requiredLong("serverNow"),
    )

    private fun parseParticipant(value: JsonValue): Participant {
        val obj = value as? JsonValue.ObjectValue ?: throw ProtocolParseException()
        return Participant(
            id = obj.requiredString("id").trim().requireSharedText(),
            name = obj.requiredString("name").trim().requireSharedText(),
            voted = obj.requiredBoolean("voted"),
            vote = obj.optionalString("vote")?.trim()?.requireSharedText(),
        )
    }

    private fun parsePokerItem(value: JsonValue): PokerItem {
        val obj = value as? JsonValue.ObjectValue ?: throw ProtocolParseException()
        val status = PokerItemStatus.fromProtocolValue(obj.requiredString("status"))
            ?: throw ProtocolParseException()
        return PokerItem(
            id = obj.requiredString("id").trim().requireSharedText(),
            label = obj.requiredString("label").trim().requireSharedText(),
            status = status,
            estimate = obj.optionalString("estimate")?.trim()?.requireSharedText(),
        )
    }

    private fun parseBucketItem(value: JsonValue): PositionedItem {
        val obj = value as? JsonValue.ObjectValue ?: throw ProtocolParseException()
        val position = obj.optionalString("position")?.trim()
        if (position != null && position !in VALID_BUCKET_POSITIONS) throw ProtocolParseException()
        return PositionedItem(
            id = obj.requiredString("id").trim().requireSharedText(),
            label = obj.requiredString("label").trim().requireSharedText(),
            position = position,
        )
    }

    private fun parseRelativeItem(value: JsonValue): PositionedItem {
        val obj = value as? JsonValue.ObjectValue ?: throw ProtocolParseException()
        return PositionedItem(
            id = obj.requiredString("id").trim().requireSharedText(),
            label = obj.requiredString("label").trim().requireSharedText(),
            position = obj.optionalString("position")?.trim(),
        )
    }

    private fun parseServerError(root: JsonValue.ObjectValue): SessionError {
        val rawCode = root.requiredString("code")
        root.optionalString("message") // read and discard — server message is untrusted
        val knownCode = ServerErrorCode.fromProtocolValue(rawCode)
        return if (knownCode != null) {
            SessionError.KnownError(code = knownCode, userMessage = knownCode.userMessage)
        } else {
            SessionError.UnknownError(userMessage = UNKNOWN_ERROR_MESSAGE)
        }
    }
}
