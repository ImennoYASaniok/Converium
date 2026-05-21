package com.course.repositories

import com.course.models.Course
import com.course.models.CourseVisibility
import com.course.models.ModerationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.data.repository.query.Param

@Repository
interface CourseRepository : JpaRepository<Course, Long> {

    fun findByOwnerId(ownerId: Long): List<Course>

    fun findByVisibilityAndModerationStatus(
        visibility: CourseVisibility,
        status: ModerationStatus
    ): List<Course>

    fun findByVisibilityAndModerationStatusAndTitleContainingIgnoreCase(
        visibility: CourseVisibility,
        status: ModerationStatus,
        title: String
    ): List<Course>

    @Query("""
        SELECT c FROM Course c 
        JOIN c.memberships m 
        WHERE m.user.id = :userId
    """)
    fun findByMemberUserId(userId: Long): List<Course>

    fun findByVisibility(visibility: CourseVisibility): List<Course>

    fun findTop10ByOrderByIdAsc(): List<Course>

    // Use derived query methods provided by Spring Data for simplicity and readability
    fun findByTitleContainingIgnoreCase(title: String): List<Course>

    fun findByDescriptionContainingIgnoreCase(description: String): List<Course>

    // Nested property search on owner.login
    fun findByOwnerLoginContainingIgnoreCase(login: String): List<Course>
}