// Course.kt
package com.course.models

import com.user.models.User
import java.time.Instant
import jakarta.persistence.*

@Entity
@Table(name = "courses")
data class Course(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(columnDefinition = "TEXT", length = 5000)
    var description: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,

    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberships: MutableList<CourseMembership> = mutableListOf(),

    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], orphanRemoval = true)
    val steps: MutableList<Step> = mutableListOf(),

    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], orphanRemoval = true)
    val edges: MutableList<StepEdge> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var visibility: CourseVisibility = CourseVisibility.FRIENDS_ONLY,

    // Только для PUBLIC
    @Enumerated(EnumType.STRING)
    var moderationStatus: ModerationStatus? = null
)
