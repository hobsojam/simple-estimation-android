package com.hobsojam.simpleestimation.data.websocket

// itemId and position values from the server contract contain no JSON special characters.
// Callers must validate against the contract before building.
internal object MoveItemMessageBuilder {
    fun build(itemId: String, position: String?): String {
        val positionJson = if (position != null) """"$position"""" else "null"
        return """{"type":"move_item","itemId":"$itemId","position":$positionJson}"""
    }
}
