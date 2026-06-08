package com.hobsojam.simpleestimation.domain.room

sealed interface SessionError {
    val userMessage: String

    data class KnownError(val code: ServerErrorCode, override val userMessage: String) :
        SessionError
    data class UnknownError(override val userMessage: String) : SessionError
}
