package com.chats.course_chat.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class IncomingMessage(
    val courseId: Long,

    @field:NotBlank
    @field:Size(max = 2000)
    val content: String
)

data class MessageResponse(
    val id: Long,
    val senderId: Long,
    val senderName: String,
    val content: String,
    val timestamp: Instant
)

data class TypingEvent(
    val courseId: Long,
    val isTyping: Boolean = true
)

data class TypingEventResponse(
    val userId: Long,
    val username: String,
    val courseId: Long,
    val isTyping: Boolean,
    val timestamp: Instant = Instant.now()
)