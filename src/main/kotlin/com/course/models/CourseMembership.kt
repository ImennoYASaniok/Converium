package com.course.models

import com.user.models.User
import jakarta.persistence.*
@Entity
@Table(
    name = "course_memberships",
    uniqueConstraints = [UniqueConstraint(columnNames = ["course_id", "user_id"])]
)
data class CourseMembership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var ability: UserAbility = UserAbility.READ,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    val grantedBy: User? = null  // кто выдал права (для аудита)
)