package com.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateUserRequest(
    @field:NotBlank
    @field:Size(min = 5, max = 40, message = "Логин должен быть в диапазоне от 5 до 40 символов")
    val login: String,

    @field:NotBlank
    @field:Email(message = "Email должен быть валидным и по структуре такой же как example@email.com")
    val email: String,

    @field:NotBlank
    val password: String
)

data class RecoveryPasswordRequest(
    @field:NotBlank
    @field:Email(message = "Email должен быть валидным и по структуре такой же как example@email.com")
    val email: String,
    val newPassword: String
)

data class UpdateUserDto(
    @field:NotBlank
    @field:Email(message = "Email должен быть валидным и по структуре такой же как example@email.com")
    var email: String,

    var profilePicture: String,

    var name: String,

    var surname: String,

    var description: String
)