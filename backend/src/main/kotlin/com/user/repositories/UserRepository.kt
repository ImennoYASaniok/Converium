package com.user.repositories

import com.user.models.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.data.repository.query.Param
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByLogin(login: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun existsByLogin(login: String): Boolean
    fun existsByEmail(email: String): Boolean

    @Query("""
        SELECT u FROM User u
        WHERE u.login LIKE %:query% 
        OR u.email LIKE %:query% 
        OR u.name LIKE %:query% 
        OR u.surname LIKE %:query%
    """)
    fun search(@Param("query") query: String): List<User>

    @Query("""
        SELECT u FROM User u
        WHERE u.login LIKE %:query%
    """)
    fun searchByLogin(@Param("query") query: String): List<User>

    @Query("""
        SELECT u FROM User u
        WHERE u.email LIKE %:query%
    """)
    fun searchByEmail(@Param("query") query: String): List<User>

    @Query("""
        SELECT u FROM User u
        WHERE u.name LIKE %:query% OR u.surname LIKE %:query%
    """)
    fun searchByName(@Param("query") query: String): List<User>

    fun findTop10ByOrderByIdAsc(): List<User>


    @Query("""
        SELECT COUNT(*) > 0 FROM User u 
        JOIN u.friends f 
        WHERE u.id = :userId 
        AND f.id = :friendId
    """)
    fun areFriends(userId: Long?, friendId: Long?): Boolean

    @Query("""
        SELECT SIZE(u.friends) FROM User u 
        WHERE u.id = :userId
    """)
    fun countFriends(@Param("userId") userId: Long?): Int

    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 
        THEN true ELSE false END 
        FROM User u WHERE u.id = :userId AND :bannedUser MEMBER OF u.bannedUsers
    """)
    fun isBanned(@Param("userId") userId: Long?, @Param("bannedUser") bannedUser: User): Boolean

    @Query("""
        SELECT SIZE(u.bannedUsers) FROM User u 
        WHERE u.id = :userId
    """)
    fun countBanned(@Param("userId") userId: Long?): Int

    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 
        THEN true ELSE false END 
        FROM User u 
        WHERE u.id = :userId 
        AND :requester MEMBER OF u.friendRequests
    """)
    fun hasFriendRequest(@Param("userId") userId: Long?, @Param("requester") requester: User): Boolean

    @Query("""
        SELECT f FROM User u 
        JOIN u.friends f 
        WHERE u.id = :userId
    """)
    fun findFriendsByUserId(@Param("userId") userId: Long?): List<User>

    @Query("""
        SELECT b FROM User u 
        JOIN u.bannedUsers b 
        WHERE u.id = :userId
    """)
    fun findBannedByUserId(@Param("userId") userId: Long?): List<User>

    @Query("""
        SELECT r FROM User u 
        JOIN u.friendRequests r 
        WHERE u.id = :userId
    """)
    fun findFriendRequestsByUserId(@Param("userId") userId: Long?): List<User>

    @Modifying
    @Query(value = """
        DELETE FROM user_friends 
        WHERE user_id = :userId 
        OR friend_id = :userId
    """, nativeQuery = true)
    fun deleteFriendsByUserId(@Param("userId") userId: Long)

    @Modifying
    @Query(value = """
        DELETE FROM user_bans 
        WHERE user_id = :userId 
        OR banned_user_id = :userId
    """, nativeQuery = true)
    fun deleteBansByUserId(@Param("userId") userId: Long)

    @Modifying
    @Query(value = """
        DELETE FROM friend_requests 
        WHERE user_id = :userId 
        OR requester_id = :userId
    """, nativeQuery = true)
    fun deleteRequestsByUserId(@Param("userId") userId: Long)
}