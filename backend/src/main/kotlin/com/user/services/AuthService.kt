package com.user.services

import com.security.JwtTokenProvider
import com.user.dtos.LoginRequest
import com.user.dtos.LoginResponse
import com.user.repositories.UserRepository
import com.user.validators.LoginValidator
import com.user.validators.PasswordValidator
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
        private val userRepository: UserRepository,
        private val passwordEncoder: PasswordEncoder,
        private val jwtTokenProvider: JwtTokenProvider
) {
    fun login(request: LoginRequest): LoginResponse {
        LoginValidator.validate(request.login)
        PasswordValidator.validate(request.password)

        val user =
                userRepository.findByLogin(request.login).orElseThrow {
                    IllegalArgumentException("Неверный логин или пароль")
                }

        val passwordValid = passwordEncoder.matches(request.password, user.password)
        require(passwordValid) { "Неверный логин или пароль" }

        val token = jwtTokenProvider.generateToken(user.id, user.login)
        return LoginResponse(accessToken = token)
    }
}
