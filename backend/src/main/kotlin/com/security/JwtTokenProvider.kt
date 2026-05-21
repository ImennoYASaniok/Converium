package com.security

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtTokenProvider(
        @Value("\${jwt.secret}") private val jwtSecret: String,
        @Value("\${jwt.expiration:86400000}") private val jwtExpiration: Long
) {
    fun generateToken(userId: Long, login: String): String {
        val expiresAtMs = Instant.now().toEpochMilli() + jwtExpiration
        val payload = "$userId:$login:$expiresAtMs"
        val payloadB64 = base64UrlEncode(payload.toByteArray(StandardCharsets.UTF_8))
        val sigB64 = base64UrlEncode(hmacSha256(payloadB64))
        return "$payloadB64.$sigB64"
    }

    fun validateToken(token: String): Boolean {
        val parts = token.split('.')
        if (parts.size != 2) return false

        val payloadB64 = parts[0]
        val sigB64 = parts[1]

        val expectedSigB64 = base64UrlEncode(hmacSha256(payloadB64))
        if (!constantTimeEquals(sigB64, expectedSigB64)) return false

        val payload = String(base64UrlDecode(payloadB64), StandardCharsets.UTF_8)
        // Разбираем payload: userId:login:expiresAt
        // Используем lastIndexOf чтобы правильно обработать login с символами ':'
        val lastColonIndex = payload.lastIndexOf(':')
        if (lastColonIndex == -1) return false

        val expiresAtStr = payload.substring(lastColonIndex + 1)
        val expiresAt = expiresAtStr.toLongOrNull() ?: return false

        // Находим предпоследний ':' для разделения userId и login
        val secondLastColonIndex = payload.lastIndexOf(':', lastColonIndex - 1)
        if (secondLastColonIndex == -1) return false

        val userIdStr = payload.substring(0, secondLastColonIndex)
        val login = payload.substring(secondLastColonIndex + 1, lastColonIndex)

        // Проверяем, что userId - это число
        userIdStr.toLongOrNull() ?: return false
        return Instant.now().toEpochMilli() <= expiresAt
    }

    fun getUserIdFromToken(token: String): Long {
        if (!validateToken(token)) {
            throw IllegalArgumentException("Invalid or expired token")
        }

        val parts = token.split('.')
        require(parts.size == 2) { "Invalid token" }

        val payload = String(base64UrlDecode(parts[0]), StandardCharsets.UTF_8)
        // Разбираем payload: userId:login:expiresAt
        val lastColonIndex = payload.lastIndexOf(':')
        require(lastColonIndex != -1) { "Invalid token" }

        val secondLastColonIndex = payload.lastIndexOf(':', lastColonIndex - 1)
        require(secondLastColonIndex != -1) { "Invalid token" }

        val userIdStr = payload.substring(0, secondLastColonIndex)
        return userIdStr.toLong()
    }

    private fun hmacSha256(data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(jwtSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    private fun base64UrlEncode(bytes: ByteArray): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun base64UrlDecode(value: String): ByteArray = Base64.getUrlDecoder().decode(value)

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
