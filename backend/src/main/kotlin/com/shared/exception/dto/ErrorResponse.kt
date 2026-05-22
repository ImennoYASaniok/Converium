package com.shared.exception.dto

import java.time.Instant

data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val errorCode: String,
    val message: String,
    val path: String? = null
)