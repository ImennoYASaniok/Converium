package com.user.validators

import com.user.exceptions.PasswordValidationException

object PasswordValidator {

    fun validate(password: String) {
        if (password.length < 5) {
            throw PasswordValidationException("Пароль должен содержать минимум 5 символов")
        }

        val hasLetter = password.any { it.isLetter() } // isLetter() поддерживает русские буквы
        val hasDigit = password.any { it.isDigit() }

        if (!hasLetter) {
            throw PasswordValidationException("Пароль должен содержать хотя бы одну букву")
        }

        if (!hasDigit) {
            throw PasswordValidationException("Пароль должен содержать хотя бы одну цифру")
        }
    }
}
