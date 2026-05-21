package com.chats.course_review.services

import com.chats.course_review.dto.CreateReviewRequest
import com.chats.course_review.dto.ReviewResponse
import com.chats.course_review.models.CourseReview
import com.chats.course_review.repositories.CourseReviewRepository
import com.course.repositories.CourseEnrollmentRepository
import com.course.repositories.CourseRepository
import com.shared.exception.AccessDeniedException
import com.shared.exception.AlreadyExistsException
import com.shared.exception.NotFoundException
import com.user.repositories.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewService(
    private val courseRepository: CourseRepository,
    private val reviewRepository: CourseReviewRepository,
    private val userRepository: UserRepository,
    private val enrollmentRepository: CourseEnrollmentRepository
) {
    @Transactional
    fun createReview(courseId: Long, userId: Long, dto: CreateReviewRequest): ReviewResponse {
        if (!enrollmentRepository.existsByCourseIdAndUserId(courseId, userId)) {
            throw AccessDeniedException("Только участники курса могут оставлять отзывы")
        }

        if (reviewRepository.existsByCourseIdAndUserId(courseId, userId)) {
            throw AlreadyExistsException("Вы уже оставляли отзыв на этот курс (Вы можете его поменять)")
        }

        val course = courseRepository.findById(courseId)
            .orElseThrow { NotFoundException(courseId, "Курс") }

        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException(userId, "Пользователь") }

        val review = CourseReview(
            course = course,
            user = user,
            rating = dto.rating,
            text = dto.text?.trim()
        )

        return toResponse(reviewRepository.save(review))
    }

    @Transactional
    fun updateReview(courseId: Long, userId: Long, dto: CreateReviewRequest): ReviewResponse {
        if (!enrollmentRepository.existsByCourseIdAndUserId(courseId, userId)) {
            throw AccessDeniedException("Только участники курса могут редактировать отзывы")
        }

        val review = reviewRepository.findByCourseIdAndUserId(courseId, userId)
            ?: throw NotFoundException(userId, "Отзыв")

        if (review.user.id != userId) {
            throw AccessDeniedException("Нельзя редактировать чужой отзыв")
        }

        review.update(dto.rating, dto.text)

        return toResponse(reviewRepository.save(review))
    }

    @Transactional(readOnly = true)
    fun getCourseReviews(courseId: Long, pageable: Pageable): Page<ReviewResponse> {
        return reviewRepository.findByCourseIdOrderByTimestampDesc(courseId, pageable)
            .map { toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getAverageRating(courseId: Long): Double? {
        return reviewRepository.getAverageRatingByCourseId(courseId)
    }

    private fun toResponse(r: CourseReview) = ReviewResponse(
        id = r.id!!,
        userId = r.user.id,
        username = r.user.name,
        rating = r.rating,
        text = r.text,
        timestamp = r.timestamp,
        isUpdated = r.isUpdated
    )
}