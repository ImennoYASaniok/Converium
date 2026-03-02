package com.user.controllers

import com.user.dtos.BannedUserDto
import com.user.dtos.CreateUserRequest
import com.user.dtos.FriendDto
import com.user.dtos.FriendRequestDto
import com.user.dtos.UpdateUserRequest
import com.user.dtos.UserDto
import com.user.services.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {
    private val errorStringAuth = "Требуется аутентификация"

    private fun getCurrentUserId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? Long
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserDto> {
        val created = userService.createUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): ResponseEntity<UserDto> {
        val currentUserId = getCurrentUserId()
        val user = userService.getUserById(id, currentUserId)
        return ResponseEntity.ok(user)
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: Long, @Valid @RequestBody request: UpdateUserRequest): ResponseEntity<UserDto> {
        val currentUserId = getCurrentUserId() ?: throw IllegalArgumentException(errorStringAuth)
        val updated = userService.updateUser(id, request, currentUserId)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        val currentUserId = getCurrentUserId() ?: throw IllegalArgumentException(errorStringAuth)
        userService.deleteUser(id, currentUserId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{userId}/friend-requests/{targetId}")
    fun sendFriendRequest(@PathVariable userId: Long, @PathVariable targetId: Long): ResponseEntity<Void> {
        val currentUserId = getCurrentUserId() ?: throw IllegalArgumentException(errorStringAuth)
        userService.sendFriendRequest(userId, targetId, currentUserId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/{userId}/friend-requests/{requesterId}/accept")
    fun acceptFriendRequest(@PathVariable userId: Long, @PathVariable requesterId: Long): ResponseEntity<Void> {
        val currentUserId = getCurrentUserId() ?: throw IllegalArgumentException(errorStringAuth)
        userService.acceptFriendRequest(userId, requesterId, currentUserId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{userId}/friend-requests/{requesterId}/reject")
    fun rejectFriendRequest(@PathVariable userId: Long, @PathVariable requesterId: Long): ResponseEntity<Void> {
        val currentUserId = getCurrentUserId() ?: throw IllegalArgumentException(errorStringAuth)
        userService.rejectFriendRequest(userId, requesterId, currentUserId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{userId}/friend-requests/{targetId}/cancel")
    fun cancelFriendRequest(@PathVariable userId: Long, @PathVariable targetId: Long): ResponseEntity<Void> {
        val currentUserId = getCurrentUserId() ?: throw IllegalArgumentException(errorStringAuth)
        userService.cancelFriendRequest(userId, targetId, currentUserId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{userId}/friend-requests/incoming")
    fun getIncomingFriendRequests(@PathVariable userId: Long): ResponseEntity<List<FriendRequestDto>> {
        val currentUserId = getCurrentUserId() ?: throw IllegalArgumentException(errorStringAuth)
        val requests = userService.getIncomingFriendRequests(userId, currentUserId)
        return ResponseEntity.ok(requests)
    }

    @DeleteMapping("/{userId}/friends/{friendId}")
    fun removeFriend(@PathVariable userId: Long, @PathVariable friendId: Long): ResponseEntity<Void> {
        val currentUserId = getCurrentUserId() ?: throw IllegalArgumentException(errorStringAuth)
        userService.removeFriend(userId, friendId, currentUserId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{userId}/friends")
    fun getFriends(@PathVariable userId: Long): ResponseEntity<List<FriendDto>> {
        val currentUserId = getCurrentUserId()
        val friends = userService.getFriends(userId, currentUserId)
        return ResponseEntity.ok(friends)
    }

    @PostMapping("/{userId}/bans/{bannedId}")
    fun banUser(@PathVariable userId: Long, @PathVariable bannedId: Long): ResponseEntity<Void> {
        val currentUserId = getCurrentUserId() ?: throw IllegalArgumentException(errorStringAuth)
        userService.banUser(userId, bannedId, currentUserId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping("/{userId}/bans/{bannedId}")
    fun unbanUser(@PathVariable userId: Long, @PathVariable bannedId: Long): ResponseEntity<Void> {
        val currentUserId = getCurrentUserId() ?: throw IllegalArgumentException(errorStringAuth)
        userService.unbanUser(userId, bannedId, currentUserId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{userId}/bans")
    fun getBannedUsers(@PathVariable userId: Long): ResponseEntity<List<BannedUserDto>> {
        val currentUserId = getCurrentUserId() ?: throw IllegalArgumentException(errorStringAuth)
        val bans = userService.getBannedUsers(userId, currentUserId)
        return ResponseEntity.ok(bans)
    }

    @GetMapping("/search")
    fun searchUsers(@RequestParam q: String): ResponseEntity<List<UserDto>> {
        val currentUserId = getCurrentUserId()
        val users = userService.searchUsers(q, currentUserId)
        return ResponseEntity.ok(users)
    }
}