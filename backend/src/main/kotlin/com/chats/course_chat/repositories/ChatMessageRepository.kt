package com.chats.course_chat.repositories

import com.chats.course_chat.models.CourseChatMessage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : JpaRepository<CourseChatMessage, Long> {
    fun findByCourseIdOrderByCreatedAtDesc(courseId: Long, pageable: Pageable): Page<CourseChatMessage>
}
