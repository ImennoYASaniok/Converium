package com.user

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service
class UserService(
    private val userProperties: UserProperties
) {
    private val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

    private val usersStorage = ConcurrentHashMap<Long, User>()
    private val idCounter = AtomicLong(1)

    fun createUser(user: User): User? {
        if (user.login.length < userProperties.minLenUser) {
            return null
        }

        if (user.password.length < userProperties.minLenPassword) {
            return null
        }

        if (userProperties.enablePasswordDigitsValidation && !user.password.any { it.isDigit() }) {
            return null
        }

        if (user.friends.size > userProperties.maxFriends) {
            return null
        }

        if (user.banUser.size > userProperties.maxBanUsers) {
            return null
        }

        val newId = idCounter.getAndIncrement()
        val newUser = user.copy(id = newId)
        usersStorage[newId] = newUser

        return newUser
    }

    fun getUserById(id: Long): User? {
        val user = usersStorage[id]
        return user
    }

    fun getAllUsers(): List<User> {
        return usersStorage.values.toList()
    }

    fun updateUser(id: Long, updatedUser: User): User? {
        val existingUser = getUserById(id) ?: return null

        if (updatedUser.login.length < userProperties.minLenUser) {
            return null
        }

        if (updatedUser.password.length < userProperties.minLenPassword) {
            return null
        }

        if (userProperties.enablePasswordDigitsValidation && !updatedUser.password.any { it.isDigit() }) {
            return null
        }

        if (updatedUser.friends.size > userProperties.maxFriends) {
            return null
        }

        if (updatedUser.banUser.size > userProperties.maxBanUsers) {
            return null
        }

        val userToUpdate = updatedUser.copy(id = id)
        usersStorage[id] = userToUpdate

        return userToUpdate
    }

    fun deleteUser(id: Long): Boolean {
        val removedUser = usersStorage.remove(id)
        if (removedUser != null) {
            usersStorage.values.forEach { user ->
                user.friends.remove(removedUser)
                user.banUser.remove(removedUser)
                user.friendRequest.remove(removedUser)
            }

            return true
        }

        return false
    }

    fun searchUsersByUsername(username: String): List<User> {
        val result = usersStorage.values.filter { it.username.contains(username, ignoreCase = true) }
        return result
    }

    fun addFriend(userId: Long, friendId: Long): Boolean {
        val user = usersStorage[userId]
        val friend = usersStorage[friendId]

        user!!.friends.add(friend!!)
        return true
    }

    fun clearStorage() {
        usersStorage.clear()
        idCounter.set(1)
    }
}