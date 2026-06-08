package com.hobsojam.simpleestimation.data.protocol

internal const val MAX_SHARED_TEXT_LENGTH = 200

internal class ProtocolParseException : IllegalArgumentException()

internal fun failParse(): Nothing = throw ProtocolParseException()

internal fun String.requireSharedText(): String =
    takeIf { it.isNotEmpty() && it.length <= MAX_SHARED_TEXT_LENGTH }
        ?: throw ProtocolParseException()

internal fun JsonValue.ObjectValue.requiredString(name: String): String =
    (values[name] as? JsonValue.StringValue)?.value ?: throw ProtocolParseException()

internal fun JsonValue.ObjectValue.optionalString(name: String): String? =
    when (val value = values[name]) {
        null, JsonValue.NullValue -> null
        is JsonValue.StringValue -> value.value
        else -> throw ProtocolParseException()
    }

internal fun JsonValue.ObjectValue.requiredBoolean(name: String): Boolean =
    (values[name] as? JsonValue.BooleanValue)?.value ?: throw ProtocolParseException()

internal fun JsonValue.ObjectValue.optionalBoolean(name: String): Boolean? =
    when (val value = values[name]) {
        null, JsonValue.NullValue -> null
        is JsonValue.BooleanValue -> value.value
        else -> throw ProtocolParseException()
    }

internal fun JsonValue.ObjectValue.requiredInt(name: String): Int {
    val number = (values[name] as? JsonValue.NumberValue)?.value ?: throw ProtocolParseException()
    return number.toIntOrNull()?.takeIf { it.toString() == number }
        ?: throw ProtocolParseException()
}

internal fun JsonValue.ObjectValue.requiredLong(name: String): Long =
    (values[name] as? JsonValue.NumberValue)?.value
        ?.takeIf { !it.contains('.') && !it.contains('e') && !it.contains('E') }
        ?.toLongOrNull()
        ?: throw ProtocolParseException()

internal fun JsonValue.ObjectValue.optionalLong(name: String): Long? =
    when (val value = values[name]) {
        null, JsonValue.NullValue -> null
        is JsonValue.NumberValue -> {
            val raw = value.value
            if (raw.contains('.') || raw.contains('e') || raw.contains('E')) {
                throw ProtocolParseException()
            }
            raw.toLongOrNull() ?: throw ProtocolParseException()
        }
        else -> throw ProtocolParseException()
    }

internal fun JsonValue.ObjectValue.requiredObject(name: String): JsonValue.ObjectValue =
    (values[name] as? JsonValue.ObjectValue) ?: throw ProtocolParseException()

internal fun JsonValue.ObjectValue.requiredArray(name: String): List<JsonValue> =
    (values[name] as? JsonValue.ArrayValue)?.values ?: throw ProtocolParseException()

internal sealed interface JsonValue {
    data class ArrayValue(val values: List<JsonValue>) : JsonValue
    data class ObjectValue(val values: Map<String, JsonValue>) : JsonValue
    data class StringValue(val value: String) : JsonValue
    data class NumberValue(val value: String) : JsonValue
    data class BooleanValue(val value: Boolean) : JsonValue
    data object NullValue : JsonValue
}

internal class JsonParser(private val source: String) {
    private var index = 0

    fun parse(): JsonValue {
        skipWhitespace()
        val value = parseValue()
        skipWhitespace()
        if (index != source.length) throw ProtocolParseException()
        return value
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        return when (peek()) {
            '[' -> parseArray()
            '{' -> parseObject()
            '"' -> JsonValue.StringValue(parseString())
            't' -> {
                if (!source.startsWith("true", index)) failParse()
                index += "true".length
                JsonValue.BooleanValue(true)
            }
            'f' -> {
                if (!source.startsWith("false", index)) failParse()
                index += "false".length
                JsonValue.BooleanValue(false)
            }
            'n' -> {
                if (!source.startsWith("null", index)) failParse()
                index += "null".length
                JsonValue.NullValue
            }
            '-', in '0'..'9' -> JsonValue.NumberValue(parseNumber())
            else -> failParse()
        }
    }

    private fun parseArray(): JsonValue.ArrayValue {
        consume('[')
        skipWhitespace()
        if (tryConsume(']')) return JsonValue.ArrayValue(emptyList())

        val values = mutableListOf<JsonValue>()
        do {
            values += parseValue()
            skipWhitespace()
        } while (tryConsume(','))
        consume(']')
        return JsonValue.ArrayValue(values)
    }

    private fun parseObject(): JsonValue.ObjectValue {
        consume('{')
        skipWhitespace()
        if (tryConsume('}')) return JsonValue.ObjectValue(emptyMap())

        val values = mutableMapOf<String, JsonValue>()
        do {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            consume(':')
            values[key] = parseValue()
            skipWhitespace()
        } while (tryConsume(','))
        consume('}')
        return JsonValue.ObjectValue(values)
    }

    private fun parseString(): String {
        consume('"')
        val builder = StringBuilder()
        while (index < source.length) {
            when (val character = source[index++]) {
                '"' -> return builder.toString()
                '\\' -> builder.append(parseEscape())
                in '\u0000'..'\u001f' -> throw ProtocolParseException()
                else -> builder.append(character)
            }
        }
        throw ProtocolParseException()
    }

    private fun parseEscape(): Char {
        if (index >= source.length) failParse()
        return when (val escaped = source[index++]) {
            '"', '\\', '/' -> escaped
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                if (index + UNICODE_ESCAPE_LENGTH > source.length) failParse()
                val hex = source.substring(index, index + UNICODE_ESCAPE_LENGTH)
                val codePoint = hex.toIntOrNull(HEX_RADIX) ?: failParse()
                index += UNICODE_ESCAPE_LENGTH
                codePoint.toChar()
            }
            else -> failParse()
        }
    }

    private fun parseNumber(): String {
        fun requireDigits() {
            val digitStart = index
            while (peek() in '0'..'9') index++
            if (digitStart == index) throw ProtocolParseException()
        }
        val start = index
        tryConsume('-')
        if (peek() == '0') {
            index++
        } else {
            requireDigits()
        }
        if (tryConsume('.')) requireDigits()
        if (peek() == 'e' || peek() == 'E') {
            index++
            if (peek() == '+' || peek() == '-') index++
            requireDigits()
        }
        return source.substring(start, index)
    }

    private fun skipWhitespace() {
        while (JSON_WHITESPACE.contains(peek() ?: return)) index++
    }

    private fun consume(expected: Char) {
        if (!tryConsume(expected)) throw ProtocolParseException()
    }

    private fun tryConsume(expected: Char): Boolean = if (peek() == expected) {
        index++
        true
    } else {
        false
    }

    private fun peek(): Char? = source.getOrNull(index)

    private companion object {
        const val UNICODE_ESCAPE_LENGTH = 4
        const val HEX_RADIX = 16
        const val JSON_WHITESPACE = " \n\r\t"
    }
}
