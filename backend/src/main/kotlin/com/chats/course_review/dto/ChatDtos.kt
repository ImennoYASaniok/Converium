package com.chats.course_review.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateReviewRequest(
    @field:Min(1) @field:Max(5)
    val rating: Int,

    @field:Size(max = 5000)
    val text: String? = null
)

data class ReviewResponse(
    val id: Long,
    val userId: Long,
    val username: String,
    val rating: Int,
    val text: String?,
    val timestamp: Instant,
    val isUpdated: Boolean
)