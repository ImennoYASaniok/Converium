package com.user.dtos

import com.user.models.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserDto(
        val id: Long,
        val login: String,
        val email: String,
        val theme: String,
        val profilePicture: String,
        val name: String,
        val surname: String,
        val description: String,
        val contacts: Set<String>,
        val friendCount: Int,
        val bannedCount: Int,
        val canEdit: Boolean, // доступ на редактирование данных пользователя
        val isFriend: Boolean,
        val isBanned: Boolean,
        val hasIncomingRequest: Boolean, // есть ли запросы в друзья от других пользователей
        val hasOutgoingRequest: Boolean // есть ли отправленные запросы в друзья
)

data class CreateUserRequest(
        @field:NotBlank @field:Size(min = 5, max = 40) val login: String,
        @field:NotBlank @field:Size(min = 5) val password: String,
        @field:Email @field:NotBlank @field:Size(max = 50) val email: String,
        @field:Size(max = 20) val theme: String? = null,
        val profilePicture: String?,
        @field:Size(max = 30) val name: String?,
        @field:Size(max = 30) val surname: String?,
        @field:Size(max = 5000) val description: String?,
        val contacts: Set<String>? = emptySet()
)

data class UpdateUserRequest(
        @field:Size(min = 5, max = 40) val login: String?,
        @field:Email @field:Size(max = 50) val email: String?,
        @field:Size(max = 20) val theme: String?,
        val profilePicture: String?,
        @field:Size(max = 30) val name: String?,
        @field:Size(max = 30) val surname: String?,
        @field:Size(max = 5000) val description: String?,
        val contacts: Set<String>?,
        @field:Size(min = 5) val newPassword: String?
)

data class UserSummaryDto(
        val id: Long,
        val login: String,
        val name: String,
        val surname: String,
        val avatar: String?
) {
    companion object {
        fun from(entity: User): UserSummaryDto {
            return UserSummaryDto(
                    id = entity.id,
                    login = entity.login,
                    name = entity.name,
                    surname = entity.surname,
                    avatar = entity.profilePicture.takeIf { it.isNotBlank() }
            )
        }
    }
}

data class UserProfileDto(
        val id: Long,
        val login: String,
        val name: String,
        val surname: String,
        val email: String,
        val description: String,
        val avatar: String?,
        val contacts: List<String>,
        val friends: List<UserSummaryDto>,
        val friendRequests: List<UserSummaryDto>
) {
    companion object {
        fun from(entity: User): UserProfileDto {
            return UserProfileDto(
                    id = entity.id,
                    login = entity.login,
                    name = entity.name,
                    surname = entity.surname,
                    email = entity.email,
                    description = entity.description,
                    avatar = entity.profilePicture.takeIf { it.isNotBlank() },
                    contacts = entity.contacts.toList(),
                    friends = entity.friends.map { UserSummaryDto.from(it) },
                    friendRequests = entity.friendRequests.map { UserSummaryDto.from(it) }
            )
        }
    }
}
