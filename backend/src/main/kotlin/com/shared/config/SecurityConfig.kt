package com.shared.config

import com.security.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    @Value("\${cors.allowed-origins}") private val corsAllowedOrigins: String,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    // Пока так
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.anyRequest().permitAll()
            }
        return http.build()
    }


//    Потом так
//    @Bean
//    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
//        http
//            .cors { it.configurationSource(corsConfigurationSource()) }
//            .csrf { it.disable() }
//            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
//            .authorizeHttpRequests { auth ->
//                auth
//                    .requestMatchers("/api/auth/**").permitAll()
//                    .requestMatchers("/api/courses/public/**").permitAll()
//                    .anyRequest().authenticated()
//            }
//            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
//        return http.build()
//    }
//
//    @Bean
//    fun corsConfigurationSource(): CorsConfigurationSource {
//        val configuration = CorsConfiguration().apply {
//            allowedOrigins = corsAllowedOrigins.split(",")
//            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
//            allowedHeaders = listOf("*")
//            allowCredentials = true
//            maxAge = 3600L
//        }
//
//        return UrlBasedCorsConfigurationSource().apply {
//            registerCorsConfiguration("/api/**", configuration)
//        }
//    }
//
//
//    @Bean
//    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
//        config.authenticationManager

}