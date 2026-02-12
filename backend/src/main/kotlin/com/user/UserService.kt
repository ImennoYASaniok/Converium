package com.user

import com.user.models.User
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

    interface LoggerStrings {
        fun getUserInfo(user: User): String
        fun getWarnShortLogin(minLenUser: Int): String
        fun getWarnShortPassword(minLenPassword: Int): String
        fun getWarnMissDigitPassword(): String
        fun getWarnManyFriends(maxFriends: Int): String
        fun getWarnManyBanUsers(maxBanUsers: Int): String
        fun getWarnNotFoundUser(id: Long): String
    }
    val loggerStrings: LoggerStrings = object: LoggerStrings {
        override fun getUserInfo(user: User): String = "id: ${user.id}, login: ${user.login}"
        override fun getWarnShortLogin(minLenUser: Int): String = "Логин слишком короткий (минимум $minLenUser символов)"
        override fun getWarnShortPassword(minLenPassword: Int): String = "Пароль слишком короткий (минимум $minLenPassword символов)"
        override fun getWarnMissDigitPassword(): String = "Пароль должен содержать хотя бы одну цифру"
        override fun getWarnManyFriends(maxFriends: Int): String = "Превышен лимит друзей $maxFriends"
        override fun getWarnManyBanUsers(maxBanUsers: Int): String = "Превышен лимит забаненных пользователей $maxBanUsers"
        override fun getWarnNotFoundUser(id: Long): String = "Пользователь id: $id не найден"
    }

    private val usersStorage = ConcurrentHashMap<Long, User>()
    private val idCounter = AtomicLong(1)

    fun createUser(user: User): User? {
        if (user.login.length < 1) {
            logger.warn(loggerStrings.getWarnShortLogin(userProperties.minLenUser))
            return null
        }

        if (user.password.length < 8) {
            logger.warn(loggerStrings.getWarnShortPassword(userProperties.minLenPassword))
            return null
        }

        if (userProperties.enablePasswordDigitsValidation && !user.password.any { it.isDigit() }) {
            logger.warn(loggerStrings.getWarnMissDigitPassword())
            return null
        }

        if (user.friends.size > userProperties.maxFriends) {
            logger.warn(loggerStrings.getWarnManyFriends(userProperties.maxFriends))
            return null
        }

        if (user.bannedUsers.size > userProperties.maxBanUsers) {
            logger.warn(loggerStrings.getWarnManyBanUsers(userProperties.maxBanUsers))
            return null
        }

        val newId = idCounter.getAndIncrement()
        val newUser = user.copy(id = newId)
        usersStorage[newId] = newUser

        logger.info("Пользователь успешно создан -> ${loggerStrings.getUserInfo(user)}")
        return newUser
    }

    fun getUserById(id: Long): User? {
        val user = usersStorage[id]
        if (user == null) {
            logger.warn(loggerStrings.getWarnNotFoundUser(id))
            return null
        } else {
            logger.info("Пользователь id: $id найден -> ${loggerStrings.getUserInfo(user)}")
        }
        return user
    }

    fun getAllUsers(): List<User> {
        logger.info("Всего пользователей: ${usersStorage.size}")
        return usersStorage.values.toList()
    }

    fun updateUser(id: Long, updatedUser: User): User? {
        val existingUser = getUserById(id) ?: return null

        if (updatedUser.login.length < userProperties.minLenUser) {
            logger.warn(loggerStrings.getWarnShortLogin(userProperties.minLenUser))
            return null
        }

        if (updatedUser.password.length < userProperties.minLenPassword) {
            logger.warn(loggerStrings.getWarnShortPassword(userProperties.minLenPassword))
            return null
        }

        if (userProperties.enablePasswordDigitsValidation && !updatedUser.password.any { it.isDigit() }) {
            logger.warn(loggerStrings.getWarnMissDigitPassword())
            return null
        }

        if (updatedUser.friends.size > userProperties.maxFriends) {
            logger.warn(loggerStrings.getWarnManyFriends(userProperties.maxFriends))
            return null
        }

        if (updatedUser.bannedUsers.size > userProperties.maxBanUsers) {
            logger.warn(loggerStrings.getWarnManyBanUsers(userProperties.maxBanUsers))
            return null
        }

        val userToUpdate = updatedUser.copy(id = id)
        usersStorage[id] = userToUpdate

        logger.info("Пользователь успешно обновлен -> ${loggerStrings.getUserInfo(userToUpdate)}")
        return userToUpdate
    }

    fun deleteUser(id: Long): Boolean {
        val removedUser = usersStorage.remove(id)
        if (removedUser != null) {
            logger.info("Пользователь успешно удален -> ${loggerStrings.getUserInfo(removedUser)}")

            usersStorage.values.forEach { user ->
                user.friends.remove(removedUser)
                user.bannedUsers.remove(removedUser)
                user.friendRequests.remove(removedUser)
            }

            return true
        }

        logger.warn(loggerStrings.getWarnNotFoundUser(id))
        return false
    }

    fun searchUsersByUsername(username: String): List<User> {
        val result = usersStorage.values.filter { it.surname.contains(username, ignoreCase = true) }
        logger.info("Найдено ${result.size} пользователей по запросу '$username'")
        return result
    }

    fun addFriend(userId: Long, friendId: Long): Boolean {
        val user = usersStorage[userId]
        val friend = usersStorage[friendId]

        if (user == null || friend == null) {
            logger.warn("Пользователь или друг не найдены")
            return false
        }

        if (user.friends.size >= userProperties.maxFriends) {
            logger.warn(loggerStrings.getWarnManyFriends(userProperties.maxFriends))
            return false
        }

        user.friends.add(friend)
        logger.info("Друг -> ${loggerStrings.getUserInfo(friend)} успешно добавлен пользователю -> ${loggerStrings.getUserInfo(user)}")
        return true
    }

    fun clearStorage() {
        usersStorage.clear()
        idCounter.set(1)
        logger.info("Хранилище пользователей очищено")
    }
}