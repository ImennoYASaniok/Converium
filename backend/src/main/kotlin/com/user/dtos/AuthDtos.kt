package com.user.dtos

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    val login: String,

    @field:NotBlank
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer"
)
