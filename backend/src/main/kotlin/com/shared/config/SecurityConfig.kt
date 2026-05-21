package com.shared.config

import com.security.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
        private val jwtAuthenticationFilter: JwtAuthenticationFilter,
        @Value("\${cors.allowed-origins}") private val corsAllowedOrigins: String,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                ex.accessDeniedHandler { _, response, _ ->
                    response.status = HttpStatus.FORBIDDEN.value()
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**", "/api/users/register").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/ws-chat").authenticated()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().denyAll()
            }
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
                CorsConfiguration().apply {
                    allowedOrigins =
                            corsAllowedOrigins.split(",").map { it.trim() }.filter {
                                it.isNotBlank()
                            }
                    allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    allowedHeaders = listOf("*")
                    allowCredentials = true
                    maxAge = 3600L
                }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
