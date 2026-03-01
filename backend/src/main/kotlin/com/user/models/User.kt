package com.user.models

import jakarta.persistence.*
import java.time.Instant
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 40)
    var login: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false, unique = true, length = 50)
    var email: String = "",

    @Column(name = "profile_picture")
    var profilePicture: String = "",

    @Column(length = 30)
    var name: String = "",

    @Column(length = 30)
    var surname: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_contacts", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "contact")
    var contacts: MutableSet<String> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_friends",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "friend_id")]
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    val friends: MutableSet<User> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_friends",
        joinColumns = [JoinColumn(name = "friend_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    protected val friendsInverse: MutableSet<User> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_bans",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "banned_user_id")]
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    val bannedUsers: MutableSet<User> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_bans",
        joinColumns = [JoinColumn(name = "banned_user_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    protected val bannedUsersInverse: MutableSet<User> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "friend_requests",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "requester_id")]
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    val friendRequests: MutableSet<User> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "friend_requests",
        joinColumns = [JoinColumn(name = "requester_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    protected val friendRequestsInverse: MutableSet<User> = mutableSetOf(),

    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id != null && other.id != null && id == other.id
    }

    override fun hashCode(): Int {
        return if (id != null) java.util.Objects.hash(id) else super.hashCode()
    }

    override fun toString(): String {
        return "User(id=$id, login='$login', email='$email')"
    }
}