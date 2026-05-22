package com.chats.course_chat.controllers

import com.chats.course_chat.services.ChatService
import com.chats.course_chat.dto.IncomingMessage
import com.chats.course_chat.dto.TypingEvent
import com.chats.course_chat.services.TypingService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class ChatWebSocketController(
    private val chatService: ChatService,
    private val typingService: TypingService,
    private val messagingTemplate: SimpMessagingTemplate
) {
    @MessageMapping("/chat.sendMessage")
    fun sendMessage(
        @Payload dto: IncomingMessage,
        principal: Principal
    ) {
        val senderId = principal.name.toLong()
        val response = chatService.saveMessage(dto, senderId)
        messagingTemplate.convertAndSend("/topic/course/${dto.courseId}", response)
    }

    @MessageMapping("/chat.typing")
    fun typing(
        @Payload event: TypingEvent,
        principal: Principal
    ) {
        val userId = principal.name.toLong()

        if (event.isTyping) {
            typingService.handleTyping(event.courseId, userId)
        } else {
            typingService.handleStopTyping(event.courseId, userId)
        }
    }
}