package com.course.repositories

import com.course.models.CourseEnrollment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CourseEnrollmentRepository : JpaRepository<CourseEnrollment, Long> {
    fun existsByCourseIdAndUserId(courseId: Long, userId: Long): Boolean
    fun findByCourseIdAndUserId(courseId: Long, userId: Long): CourseEnrollment?
    fun countByCourseId(courseId: Long): Int
}