package com.chats.course_chat.controllers

import com.chats.course_chat.dto.MessageResponse
import com.chats.course_chat.services.ChatService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/courses/{courseId}/chat")
class ChatRestController(
    private val chatService: ChatService
) {
    @GetMapping("/history")
    fun getHistory(
        @PathVariable courseId: Long,
        @AuthenticationPrincipal userId: Long,
        @PageableDefault(size = 50, sort = ["createdAt"]) pageable: Pageable
    ): ResponseEntity<Page<MessageResponse>> {
        return ResponseEntity.ok(chatService.getHistory(courseId, userId, pageable))
    }





}