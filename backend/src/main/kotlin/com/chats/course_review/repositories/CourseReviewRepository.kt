package com.chats.course_review.repositories

import com.chats.course_review.models.CourseReview
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CourseReviewRepository : JpaRepository<CourseReview, Long> {
    fun existsByCourseIdAndUserId(courseId: Long, userId: Long): Boolean
    fun findByCourseIdAndUserId(courseId: Long, userId: Long): CourseReview?
    fun findByCourseIdOrderByTimestampDesc(courseId: Long, pageable: Pageable): Page<CourseReview>

    @Query("SELECT AVG(r.rating) FROM CourseReview r WHERE r.course.id = :courseId")
    fun getAverageRatingByCourseId(courseId: Long): Double?
}