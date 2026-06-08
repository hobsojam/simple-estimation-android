package com.hobsojam.simpleestimation.domain.room

sealed interface SessionError {
    data class KnownError(val code: ServerErrorCode, val userMessage: String) : SessionError
    data class UnknownError(val userMessage: String) : SessionError
}
