package com.user

import com.user.dtos.CreateUserRequest
import com.user.repositories.UserRepository
import com.user.services.UserService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
@Profile("test-runner")
class TestUserRunner(
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val transactionTemplate: TransactionTemplate
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val TEST_LOGINS = listOf("user_1", "user_2", "user_3")
    }

    @PostConstruct
    fun init() {
        println("TestUserRunner создание")
    }

    override fun run(args: ApplicationArguments?) {
        log.info("=== Запуск проверки сервиса пользователей ===")

        try {
            transactionTemplate.executeWithoutResult {
                deleteTestUsers()
            }

            val user1 = createUser(TEST_LOGINS[0], "Alice", "Smith")
            val user2 = createUser(TEST_LOGINS[1], "Bob", "Johnson")
            val user3 = createUser(TEST_LOGINS[2], "Charlie", "Brown")

            log.info("Созданы пользователи: id1=${user1.id}, id2=${user2.id}, id3=${user3.id}")

            testFriendsAndBans(user1, user2, user3)

            log.info("=== Проверка завершена успешно ===")

        } catch (e: Exception) {
            log.error("Ошибка при выполнении проверки: ", e)
        }
    }

    private fun deleteTestUsers() {
        TEST_LOGINS.forEach { login ->
            userRepository.findByLogin(login).ifPresent { user ->
                userRepository.deleteFriendsByUserId(user.id)
                userRepository.deleteBansByUserId(user.id)
                userRepository.deleteRequestsByUserId(user.id)
                userRepository.delete(user)
                log.info("Удалён старый тестовый пользователь с логином: $login")
            }
        }
    }

    private fun createUser(login: String, name: String, surname: String): com.user.dtos.UserDto {
        val request = CreateUserRequest(
            login = login,
            password = "password123",
            email = "$login@test.com",
            profilePicture = null,
            name = name,
            surname = surname,
            description = "Test user $login",
            contacts = setOf("telegram: @$login", "phone: +123456789")
        )
        return userService.createUser(request)
    }

    private fun testFriendsAndBans(
        user1: com.user.dtos.UserDto,
        user2: com.user.dtos.UserDto,
        user3: com.user.dtos.UserDto
    ) {
        val profile1 = userService.getUserById(user1.id, null)
        log.info("Профиль user1 (аноним): email=${profile1.email}, contacts=${profile1.contacts}")
        val profile1owner = userService.getUserById(user1.id, user1.id)
        log.info("Профиль user1 (владелец): email=${profile1owner.email}, contacts=${profile1owner.contacts}")

        userService.sendFriendRequest(user1.id, user2.id, user1.id)
        log.info("Заявка от user1 к user2 отправлена")

        val incoming = userService.getIncomingFriendRequests(user2.id, user2.id)
        log.info("У user2 входящих заявок: ${incoming.size} (должна быть 1)")

        userService.acceptFriendRequest(user2.id, user1.id, user2.id)
        log.info("Заявка принята")

        val friends1 = userService.getFriends(user1.id, user1.id)
        val friends2 = userService.getFriends(user2.id, user2.id)
        log.info("Друзья user1: ${friends1.map { it.user.login }}")
        log.info("Друзья user2: ${friends2.map { it.user.login }}")

        try {
            userService.sendFriendRequest(user1.id, user2.id, user1.id)
        } catch (e: Exception) {
            log.info("Ожидаемая ошибка при повторной заявке: ${e.message}")
        }

        val user1viewByUser2 = userService.getUserById(user1.id, user2.id)
        log.info("user1 глазами user2: isFriend=${user1viewByUser2.isFriend} (должно быть true)")

        try {
            userService.sendFriendRequest(user3.id, user1.id, user1.id)
        } catch (e: Exception) {
            log.info("Ожидаемая ошибка при отправке от чужого имени: ${e.message}")
        }

        userService.banUser(user1.id, user3.id, user1.id)
        log.info("user1 забанил user3")
        val bannedList = userService.getBannedUsers(user1.id, user1.id)
        log.info("Бан-лист user1: ${bannedList.map { it.bannedUser.login }}")

        try {
            userService.sendFriendRequest(user3.id, user1.id, user3.id)
        } catch (e: Exception) {
            log.info("Ожидаемая ошибка при отправке заявки от забаненного: ${e.message}")
        }

        userService.unbanUser(user1.id, user3.id, user1.id)
        log.info("user1 разбанил user3")

        val searchResults = userService.searchUsers("user", null, null)
        log.info("Поиск 'user': найдено ${searchResults.size} пользователей")

        userService.deleteUser(user3.id, user3.id)
        log.info("Пользователь user3 удалён")

        try {
            userService.getUserById(user3.id, null)
        } catch (e: Exception) {
            log.info("Ожидаемая ошибка при получении удалённого пользователя: ${e.message}")
        }
    }
}
