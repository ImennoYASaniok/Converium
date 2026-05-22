package com.chats.course_chat.services

import com.chats.course_chat.dto.TypingEventResponse
import com.shared.exception.AccessDeniedException
import com.course.repositories.CourseEnrollmentRepository
import com.user.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class TypingService(
    private val enrollmentRepository: CourseEnrollmentRepository,
    private val userRepository: UserRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = LoggerFactory.getLogger(TypingService::class.java)

    private val activeTypers = ConcurrentHashMap<String, Instant>()

    fun handleTyping(courseId: Long, userId: Long) {
        checkAccess(courseId, userId)

        val key = "$courseId:$userId"
        val wasTyping = activeTypers.containsKey(key)

        activeTypers[key] = Instant.now()

        if (!wasTyping) {
            broadcast(courseId, userId, true)
        }
    }

    fun handleStopTyping(courseId: Long, userId: Long) {
        val key = "$courseId:$userId"
        if (activeTypers.remove(key) != null) {
            broadcast(courseId, userId, false)
        }
    }


    @Scheduled(fixedRate = 5000)
    fun cleanup() {
        val now = Instant.now()
        val expired = activeTypers.entries.filter {
            Duration.between(it.value, now).seconds > 5
        }

        expired.forEach { (key, _) ->
            activeTypers.remove(key)
            val (courseId, userId) = key.split(":").map { it.toLong() }
            broadcast(courseId, userId, false)
            logger.debug("Автоматический сброс состояния \"Печатает...\" у пользователя {} в чате курса {}", userId, courseId)
        }
    }

    private fun checkAccess(courseId: Long, userId: Long) {
        if (!enrollmentRepository.existsByCourseIdAndUserId(courseId, userId)) {
            throw AccessDeniedException("Нет доступа к чату курса")
        }
    }

    private fun broadcast(courseId: Long, userId: Long, isTyping: Boolean) {
        val user = userRepository.findById(userId).orElse(null) ?: return
        messagingTemplate.convertAndSend(
            "/topic/course/$courseId/typing",
            TypingEventResponse(
                userId = userId,
                username = user.name,
                courseId = courseId,
                isTyping = isTyping
            )
        )
    }
}