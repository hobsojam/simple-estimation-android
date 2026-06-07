package com.hobsojam.simpleestimation.data.websocket

internal object RoomJoinMessageBuilder {
    fun build(displayName: String, accessPin: String?): String = buildString {
        append("""{"type":"join","name":""")
        append(jsonString(displayName))
        if (accessPin != null) {
            append(""","accessPin":""")
            append(jsonString(accessPin))
        }
        append("}")
    }

    private fun jsonString(value: String): String = buildString {
        append('"')
        for (char in value) {
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '' -> append("\\f")
                else -> if (char.code < CONTROL_CHAR_THRESHOLD) {
                    append("\\u${char.code.toString(HEX_RADIX).padStart(UNICODE_ESCAPE_WIDTH, '0')}")
                } else {
                    append(char)
                }
            }
        }
        append('"')
    }

    private const val CONTROL_CHAR_THRESHOLD = 0x20
    private const val HEX_RADIX = 16
    private const val UNICODE_ESCAPE_WIDTH = 4
}
