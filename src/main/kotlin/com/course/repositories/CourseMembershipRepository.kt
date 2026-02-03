package com.course.repositories

import com.course.models.CourseMembership
import com.course.models.UserAbility
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CourseMembershipRepository : JpaRepository<CourseMembership, Long> {

    fun findByCourseIdAndUserId(courseId: Long, userId: Long): CourseMembership?

    fun existsByCourseIdAndUserId(courseId: Long, userId: Long): Boolean

    fun findByCourseId(courseId: Long): List<CourseMembership>

    fun findByUserIdAndAbility(userId: Long, ability: UserAbility): List<CourseMembership>

    fun deleteByCourseIdAndUserId(courseId: Long, userId: Long)
}