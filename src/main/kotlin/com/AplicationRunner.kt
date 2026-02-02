package com

import com.user.User
import com.user.UserProperties
import com.user.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class ApplicationRunner(
    private val userService: UserService,
    private val userProperties: UserProperties
) : CommandLineRunner {

    private val logger: Logger = LoggerFactory.getLogger(ApplicationRunner::class.java)

    override fun run(vararg args: String?) {
        userService.clearStorage()

        logger.info("\n--- 1) СОЗДАНИЕ ПОЛЬЗОВАТЕЛЕЙ ---")
        logger.info("Создание пользователя")
        val user1 = User(
            login = "prosto_saniok",
            password = "password123",
            email = "prosto_saniok@example.com",
            name = "Prosto",
            username = "Saniok"
        )
        val createdUser1 = userService.createUser(user1)

        logger.info("Создание пользователя с коротким паролем")
        val user2 = User(
            login = "shortpass",
            password = "123",
            email = "short@example.com"
        )
        val createdUser2 = userService.createUser(user2)

        val user3 = User(
            login = "noname",
            password = "noname123",
            email = "no_name@example.com",
            name = "NoName",
            profilePicture = "avatar.jpg"
        )
        val createdUser3 = userService.createUser(user3)

        val user4 = User(
            login = "prosto_saniok_dublicate",
            password = "password321",
            email = "prosto_saniok@example.com",
            name = "Prosto",
            username = "Saniok"
        )
        val createdUser4 = userService.createUser(user4)


        logger.info("\n--- 2) ЧТЕНИЕ ПОЛЬЗОВАТЕЛЕЙ ---")

        val allUsers = userService.getAllUsers()
        for (user in allUsers) {
            logger.info(userService.loggerStrings.getUserInfo(user))
        }

        val retrievedUser = userService.getUserById(createdUser1?.id ?: 0)
        val warnRetrievedUser = userService.getUserById(9999)


        logger.info("\n--- 3) ПОИСК ПОЛЬЗОВАТЕЛЕЙ ---")
        val searchResults = userService.searchUsersByUsername("Saniok")


        logger.info("\n--- 4) ОБНОВЛЕНИЕ ПОЛЬЗОВАТЕЛЯ ---")
        if (createdUser1 != null) {
            val updatedUser = createdUser1.copy(
                name = "Saniok Updated",
                email = "updated@example.com"
            )
            val updateResult = userService.updateUser(createdUser1.id, updatedUser)
        }


        logger.info("\n--- 5) ДОБАВЛЕНИЕ ДРУЗЕЙ ---")
        val friendAdded = userService.addFriend(createdUser1?.id ?: 0, createdUser3?.id ?: 0)


        logger.info("\n--- 6) УДАЛЕНИЕ ПОЛЬЗОВАТЕЛЯ ---")
        val deleted = userService.deleteUser(createdUser1?.id ?: 0)
        val checkDeleted = userService.getUserById(createdUser1?.id ?: 0)

        logger.info("\n--- 6) ИТОГ ---")
        val finalUsers = userService.getAllUsers()
        for (user in finalUsers) {
            logger.info(userService.loggerStrings.getUserInfo(user))
        }
    }
}