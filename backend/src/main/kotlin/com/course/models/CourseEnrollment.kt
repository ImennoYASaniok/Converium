package com.course.models

import com.user.models.User
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "course_enrollments",
    uniqueConstraints = [UniqueConstraint(columnNames = ["course_id", "user_id"])]
)
data class CourseEnrollment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    val enrolledAt: Instant = Instant.now()
)