package com.user.repositories

import com.user.models.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {

    // Для логина
    fun findByLogin(login: String): User?

    // Проверка существования
    fun existsByLogin(login: String): Boolean
    fun existsByUsername(username: String): Boolean

    @Query("SELECT COUNT(*) > 0 FROM User u JOIN u.friends f WHERE u.id = :userId AND f.id = :friendId")
    fun areFriends(userId: Long, friendId: Long): Boolean
}