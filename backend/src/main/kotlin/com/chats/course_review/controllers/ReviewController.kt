package com.chats.course_review.controllers

import com.chats.course_review.dto.CreateReviewRequest
import com.chats.course_review.dto.ReviewResponse
import com.chats.course_review.services.ReviewService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/courses/{courseId}/reviews")
class ReviewController(
    private val reviewService: ReviewService
) {

    @PostMapping
    fun createReview(
        @PathVariable courseId: Long,
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody dto: CreateReviewRequest
    ): ResponseEntity<ReviewResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(reviewService.createReview(courseId, userId, dto))
    }

    @GetMapping
    fun getReviews(
        @PathVariable courseId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<ReviewResponse>> {
        return ResponseEntity.ok(reviewService.getCourseReviews(courseId, pageable))
    }


    @PutMapping("/my")
    fun updateReview(
        @PathVariable courseId: Long,
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody dto: CreateReviewRequest
    ): ResponseEntity<ReviewResponse> {
        return ResponseEntity.ok(reviewService.updateReview(courseId, userId, dto))
    }

    @GetMapping("/average-rating")
    fun averageRating(@PathVariable courseId: Long): ResponseEntity<Double> {
        return ResponseEntity.ok(reviewService.getAverageRating(courseId))
    }
}