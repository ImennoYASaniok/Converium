package com.user.models

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    var login: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false, unique = true, length = 100)
    var email: String = "",

    @Column(name = "profile_picture", length = 500)
    var profilePicture: String = "",

    @Column(length = 100)
    var name: String = "",

    @Column(length = 50, unique = true)
    var username: String = "",

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
    val friends: MutableSet<User> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_bans",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "banned_user_id")]
    )
    val bannedUsers: MutableSet<User> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "friend_requests",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "requester_id")]
    )
    val friendRequests: MutableSet<User> = mutableSetOf(),

    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now()
)