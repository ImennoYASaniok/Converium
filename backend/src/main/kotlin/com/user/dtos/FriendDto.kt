package com.user.dtos

data class FriendDto(
    val user: UserDto
)

data class FriendRequestDto(
    val requester: UserDto
)