package com.chats.course_chat.models

import com.course.models.Course
import com.user.models.User
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "course_chat_messages")
class CourseChatMessage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: User,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
)