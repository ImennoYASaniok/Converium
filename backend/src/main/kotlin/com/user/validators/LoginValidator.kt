package com.user.validators

import com.user.exceptions.LoginValidationException

object LoginValidator {

    fun validate(login: String) {
        if (login.length < 5) {
            throw LoginValidationException("Логин должен содержать минимум 5 символов")
        }

        // Проверяем, что логин начинается с буквы (включая русские)
        if (!login[0].isLetter()) {
            throw LoginValidationException("Логин должен начинаться с буквы")
        }

        // Проверяем, что содержатся только буквы (включая русские), цифры, _ и -
        val validChars = Regex("^[a-zA-Zа-яА-ЯёЁ0-9_-]+$")
        if (!validChars.matches(login)) {
            throw LoginValidationException(
                    "Логин может содержать только буквы, цифры, символы '_' и '-'"
            )
        }
    }
}
