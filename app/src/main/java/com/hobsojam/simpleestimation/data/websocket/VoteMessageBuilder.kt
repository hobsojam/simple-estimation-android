package com.hobsojam.simpleestimation.data.websocket

// Vote values from the server contract contain no JSON special characters.
// Callers must validate against the contract before building.
internal object VoteMessageBuilder {
    fun build(vote: String): String = """{"type":"vote","vote":"$vote"}"""
}
