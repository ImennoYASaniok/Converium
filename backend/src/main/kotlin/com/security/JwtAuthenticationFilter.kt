package com.security

import com.user.repositories.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/users/register")
                || path.startsWith("/actuator/health")
    }

    override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
    ) {
        val token = getTokenFromRequest(request)

        if (token != null && jwtTokenProvider.validateToken(token)) {
            val userId = jwtTokenProvider.getUserIdFromToken(token)

            // Проверяем, существует ли пользователь в базе данных
            val userExists = userRepository.existsById(userId)
            
            if (userExists) {
                val authentication = UsernamePasswordAuthenticationToken(userId, null, emptyList())
                SecurityContextHolder.getContext().authentication = authentication
            } else {
                // Если пользователь не существует, очищаем контекст безопасности
                SecurityContextHolder.clearContext()
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun getTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}
