package com.user.services

import com.user.dtos.BannedUserDto
import com.user.properties.UserProperties
import com.user.dtos.CreateUserRequest
import com.user.dtos.FriendDto
import com.user.dtos.FriendRequestDto
import com.user.dtos.UpdateUserRequest
import com.user.dtos.UserDto
import com.user.models.User
import com.user.repositories.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userProperties: UserProperties
) {
    private fun validatePassword(password: String) {
        require(password.length >= userProperties.minLenPassword) {
            "Пароль должен быть не менее ${userProperties.minLenPassword} символов"
        }
        if (userProperties.enablePasswordDigitsValidation) {
            require(password.any { it.isDigit() }) {
                "Пароль должен состоять хотя бы из одной цифры"
            }
        }
    }

    private fun validateLogin(login: String) {
        require(login.length >= userProperties.minLenUser) {
            "Логин должен быть не менее ${userProperties.minLenUser} символов"
        }
    }

    private fun getUserById(id: Long): User {
        return userRepository.findById(id).orElseThrow { IllegalArgumentException("Пользователь не найден") }
    }

    fun createUser(request: CreateUserRequest): UserDto {
        require(!userRepository.existsByLogin(request.login)) { "Логин уже создан" }
        require(!userRepository.existsByEmail(request.email)) { "Email уже создан" }

        validateLogin(request.login)
        validatePassword(request.password)

        val user = User(
            login = request.login,
            password = passwordEncoder.encode(request.password),
            email = request.email,
            profilePicture = request.profilePicture ?: "",
            name = request.name ?: "",
            surname = request.surname ?: "",
            description = request.description ?: "",
            contacts = request.contacts?.toMutableSet() ?: mutableSetOf()
        )
        val saved = userRepository.save(user)
        return buildUserDto(saved, saved.id!!)
    }

    fun getUserById(id: Long, currentUserId: Long?): UserDto {
        val user = getUserById(id)
        return buildUserDto(user, currentUserId)
    }

    fun updateUser(id: Long, request: UpdateUserRequest, currentUserId: Long): UserDto {
        val user = getUserById(id)
        val currentUser = getUserById(currentUserId)

        require(currentUserId == id) { "Ты не можешь редактировать этого пользователя" }

        request.login?.let { newLogin ->
            require(!userRepository.existsByLogin(newLogin) || user.login == newLogin) { "Логин уже используется" }
            validateLogin(newLogin)
            user.login = newLogin
        }
        request.email?.let { newEmail ->
            require(!userRepository.existsByEmail(newEmail) || user.email == newEmail) { "Email уже используется" }
            user.email = newEmail
        }
        request.profilePicture?.let { user.profilePicture = it }
        request.name?.let { user.name = it }
        request.surname?.let { user.surname = it }
        request.description?.let { user.description = it }
        request.contacts?.let { user.contacts = it.toMutableSet() }
        request.newPassword?.let { newPassword ->
            validatePassword(newPassword)
            user.password = passwordEncoder.encode(newPassword)
        }

        val saved = userRepository.save(user)
        return buildUserDto(saved, currentUserId)
    }

    fun deleteUser(id: Long, currentUserId: Long) {
        val user = getUserById(id)
        val currentUser = getUserById(currentUserId)
        require(currentUserId == id) { "Ты не можешь удалить этого пользователя" }

        userRepository.deleteFriendsByUserId(id)
        userRepository.deleteBansByUserId(id)
        userRepository.deleteRequestsByUserId(id)

        userRepository.delete(user)
    }

    fun searchUsers(query: String, currentUserId: Long?): List<UserDto> {
        return userRepository.search(query).map { buildUserDto(it, currentUserId) }
    }

    fun sendFriendRequest(fromUserId: Long, toUserId: Long, currentUserId: Long) {
        require(currentUserId == fromUserId) { "Отправка заявки можно делать только со своего пользователя" }
        require(fromUserId != toUserId) { "Нельзя отправить ссылку в друзья самому себе" }

        val fromUser = getUserById(fromUserId)
        val toUser = getUserById(toUserId)

        require(!userRepository.isBanned(fromUserId, toUser)) { "Ты пытаешься добавить " }
        require(!userRepository.isBanned(toUserId, fromUser)) { "Этот пользователь забанил тебя" }
        require(!userRepository.areFriends(fromUserId, toUser.id)) { "Этот пользователь уже добавлен в друзья" }
        require(!userRepository.hasFriendRequest(toUserId, fromUser)) { "Запрос в друзья для этоого пользователя уже отправлен" }
        require(!userRepository.hasFriendRequest(fromUserId, toUser)) { "Этот пользователь уже отправил вам запрос в друзья" }

        toUser.friendRequests.add(fromUser)
        userRepository.save(toUser)
    }

    fun acceptFriendRequest(acceptorUserId: Long, requesterId: Long, currentUserId: Long) {
        require(currentUserId == acceptorUserId) { "Приём заявки в друзья можно делать только со своего пользователя" }

        val acceptor = getUserById(acceptorUserId)
        val requester = getUserById(requesterId)

        require(userRepository.hasFriendRequest(acceptorUserId, requester)) { "Нет запросов в друзья у отправителя" }

        require(userRepository.countFriends(acceptorUserId) < userProperties.maxFriends) {
            "У вас достигнуто максимальное кол-во друзей (${userProperties.maxFriends})"
        }
        require(userRepository.countFriends(requesterId) < userProperties.maxFriends) {
            "У пользователя достигнуто максимальное кол-во друзей (${userProperties.maxFriends})"
        }

        acceptor.friends.add(requester)
        requester.friends.add(acceptor)

        acceptor.friendRequests.remove(requester)

        userRepository.saveAll(listOf(acceptor, requester))
    }

    fun rejectFriendRequest(rejectorUserId: Long, requesterId: Long, currentUserId: Long) {
        require(currentUserId == rejectorUserId) { "Вы можете отклонять заявки только с вашего аккаунта" }

        val rejector = getUserById(rejectorUserId)
        val requester = getUserById(requesterId)

        require(userRepository.hasFriendRequest(rejectorUserId, requester)) { "Нет запросов для этого пользователя на отклонение" }

        rejector.friendRequests.remove(requester)
        userRepository.save(rejector)
    }

    fun cancelFriendRequest(cancelerUserId: Long, targetUserId: Long, currentUserId: Long) {
        require(currentUserId == cancelerUserId) { "Вы можете отменить заявки только с вашего аккаунта" }

        val canceler = getUserById(cancelerUserId)
        val target = getUserById(targetUserId)

        require(userRepository.hasFriendRequest(targetUserId, canceler)) { "Нет заявок для отмены на этого пользователя" }

        target.friendRequests.remove(canceler)
        userRepository.save(target)
    }

    fun removeFriend(userId: Long, friendId: Long, currentUserId: Long) {
        require(currentUserId == userId) { "Вы можете удалять друзей только с вашего аккаунта" }

        val user = getUserById(userId)
        val friend = getUserById(friendId)

        require(userRepository.areFriends(userId, friend.id)) { "Вы не можете удалить этого пользователя из друзей, так как он изначально у вас не в друзьях" }

        user.friends.remove(friend)
        friend.friends.remove(user)

        userRepository.saveAll(listOf(user, friend))
    }

    fun getFriends(userId: Long, currentUserId: Long?): List<FriendDto> {
        val friends = userRepository.findFriendsByUserId(userId)
        return friends.map { friend ->
            FriendDto(user = buildUserDto(friend, currentUserId))
        }
    }

    fun getIncomingFriendRequests(userId: Long, currentUserId: Long?): List<FriendRequestDto> {
        require(currentUserId == userId) { "Вы можете увидеть свои запросы в друзья только с вашего аккаунта" }
        val requesters = userRepository.findFriendRequestsByUserId(userId)
        return requesters.map { requester ->
            FriendRequestDto(requester = buildUserDto(requester, currentUserId))
        }
    }

    fun banUser(userId: Long, bannedUserId: Long, currentUserId: Long) {
        require(currentUserId == userId) { "Вы можете банить пользователей только с вашего аккаунта" }
        require(userId != bannedUserId) { "Вы не можете забанить себя" }

        val user = getUserById(userId)
        val bannedUser = getUserById(bannedUserId)

        val currentBans = userRepository.countBanned(userId)
        require(currentBans < userProperties.maxBanUsers) {
            "Вы достигли лимита по бану пользователей (${userProperties.maxBanUsers} пользователей)"
        }

        require(!userRepository.isBanned(userId, bannedUser)) { "Этот пользователь уже вами забанен" }

        if (userRepository.areFriends(userId, bannedUser.id)) {
            user.friends.remove(bannedUser)
            bannedUser.friends.remove(user)
        }

        user.friendRequests.remove(bannedUser)
        bannedUser.friendRequests.remove(user)

        user.bannedUsers.add(bannedUser)

        userRepository.saveAll(listOf(user, bannedUser))
    }

    fun unbanUser(userId: Long, bannedUserId: Long, currentUserId: Long) {
        require(currentUserId == userId) { "Вы можете разбанить пользователя только с вашего аккаунта" }

        val user = getUserById(userId)
        val bannedUser = getUserById(bannedUserId)

        require(userRepository.isBanned(userId, bannedUser)) { "Пользователь не был забанен" }

        user.bannedUsers.remove(bannedUser)
        userRepository.save(user)
    }

    fun getBannedUsers(userId: Long, currentUserId: Long?): List<BannedUserDto> {
        require(currentUserId == userId) { "Вы можете посмотреть список забанненых пользователей только с вашего аккаунта" }
        val banned = userRepository.findBannedByUserId(userId)
        return banned.map { bannedUser ->
            BannedUserDto(bannedUser = buildUserDto(bannedUser, currentUserId))
        }
    }

    private fun buildUserDto(user: User, currentUserId: Long?): UserDto {
        val userId = user.id!!
        val isCurrentUser = currentUserId == userId
        val isFriend = currentUserId != null && userRepository.areFriends(currentUserId, userId)
        val isBanned = currentUserId != null && userRepository.isBanned(currentUserId, user)
        val hasIncomingRequest = currentUserId != null && userRepository.hasFriendRequest(userId, getUserById(currentUserId))
        val hasOutgoingRequest = currentUserId != null && userRepository.hasFriendRequest(currentUserId, user)

        val friendCount = userRepository.countFriends(userId)
        val bannedCount = userRepository.countBanned(userId)

        return UserDto(
            id = userId,
            login = user.login,
            email = user.email,
            profilePicture = user.profilePicture,
            name = user.name,
            surname = user.surname,
            description = user.description,
            contacts = HashSet(user.contacts),
            friendCount = friendCount,
            bannedCount = bannedCount,
            canEdit = isCurrentUser,
            isFriend = isFriend,
            isBanned = isBanned,
            hasIncomingRequest = hasIncomingRequest,
            hasOutgoingRequest = hasOutgoingRequest
        )
    }
}