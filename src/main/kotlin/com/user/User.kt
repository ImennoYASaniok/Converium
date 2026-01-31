package com.user

// Модель пользователя

data class User (
    val id: Long,
    var login: String,
    var avatar: String = "", // путь до картинки
    var name: String = "",
    var username: String = "",
    var description: String = "", // описание
    var contacts: MutableList<String> = mutableListOf(), // контакты, которые может добавить человек (2-3 контакта) (к примеру его почта, телефон)
)

// Также есть модели Friend, FriendRequest, BanFriend, которые использует id User