package com.user

data class User(
    val id: Long = 0,

    var login: String,

    var password: String,

    var email: String = "",

    var profilePicture: String = "", // путь до картинки

    var name: String = "",

    var username: String = "",

    var description: String = "", // описание

    var contacts: MutableSet<String> = mutableSetOf(), // контакты

    val friends: MutableSet<User> = mutableSetOf(),

    val banUser: MutableSet<User> = mutableSetOf(),

    val friendRequest: MutableSet<User> = mutableSetOf()
)