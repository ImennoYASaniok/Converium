package com.user.dtos

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserDto(
    val id: Long,
    val login: String,
    val email: String,
    val profilePicture: String,
    val name: String,
    val surname: String,
    val description: String,
    val contacts: Set<String>,
    val friendCount: Int,
    val bannedCount: Int,
    val canEdit: Boolean,
    val isFriend: Boolean,
    val isBanned: Boolean,
    val hasIncomingRequest: Boolean,
    val hasOutgoingRequest: Boolean
)

data class CreateUserRequest(
    @field:NotBlank @field:Size(min = 5, max = 40)
    val login: String,

    @field:NotBlank @field:Size(min = 5)
    val password: String,

    @field:Email @field:NotBlank @field:Size(max = 50)
    val email: String,

    @field:Size(max = 255)
    val profilePicture: String?,

    @field:Size(max = 30)
    val name: String?,

    @field:Size(max = 30)
    val surname: String?,

    @field:Size(max = 5000)
    val description: String?,

    val contacts: Set<String>? = emptySet()
)

data class UpdateUserRequest(
    @field:Size(min = 5, max = 40)
    val login: String?,

    @field:Email @field:Size(max = 50)
    val email: String?,

    @field:Size(max = 255)
    val profilePicture: String?,

    @field:Size(max = 30)
    val name: String?,

    @field:Size(max = 30)
    val surname: String?,

    @field:Size(max = 5000)
    val description: String?,

    val contacts: Set<String>?,

    @field:Size(min = 5)
    val newPassword: String?
)