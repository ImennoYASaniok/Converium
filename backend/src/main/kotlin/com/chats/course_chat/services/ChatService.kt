package com.chats.course_chat.services

import com.chats.course_chat.models.CourseChatMessage
import com.chats.course_chat.repositories.ChatMessageRepository
import com.chats.course_chat.dto.IncomingMessage
import com.chats.course_chat.dto.MessageResponse
import com.course.repositories.CourseEnrollmentRepository
import com.course.repositories.CourseRepository
import com.shared.exception.AccessDeniedException
import com.shared.exception.NotFoundException
import com.user.repositories.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val chatRepository: ChatMessageRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val enrollmentRepository: CourseEnrollmentRepository
) {

    @Transactional
    fun saveMessage(dto: IncomingMessage, senderId: Long): MessageResponse {
        if (!enrollmentRepository.existsByCourseIdAndUserId(dto.courseId, senderId)) {
            throw AccessDeniedException("Вы не записаны на этот курс")
        }

        val course = courseRepository.findById(dto.courseId)
            .orElseThrow { NotFoundException(dto.courseId, "Курс") }

        val sender = userRepository.findById(senderId)
            .orElseThrow {
                NotFoundException(
                    senderId,
                    "Пользователь",
                    "Отправитель сообщения не найден (id=$senderId)"
                )
            }

        val message = CourseChatMessage(
            course = course,
            sender = sender,
            content = dto.content.trim()
        )

        val saved = chatRepository.save(message)
        return tResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getHistory(courseId: Long, userId: Long, pageable: Pageable): Page<MessageResponse> {
        if (!enrollmentRepository.existsByCourseIdAndUserId(courseId, userId)) {
            throw AccessDeniedException("Нет доступа к чату курса")
        }

        return chatRepository.findByCourseIdOrderByCreatedAtDesc(courseId, pageable)
            .map { tResponse(it) }
    }


    private fun tResponse(msg: CourseChatMessage) = MessageResponse(
        id = msg.id!!,
        senderId = msg.sender.id,
        senderName = msg.sender.name,
        content = msg.content,
        timestamp = msg.createdAt
    )

}