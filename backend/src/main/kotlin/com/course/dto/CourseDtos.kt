package com.course.dto

import com.course.models.CourseEnrollment
import com.course.models.CourseVisibility
import com.course.models.ModerationStatus
import com.course.models.StepType
import java.time.Instant

data class CreateCourseDto(
    val title: String,
    val description: String?,
    val visibility: CourseVisibility
)

data class UpdateCourseDto(
    val title: String?,
    val description: String?,
    val visibility: CourseVisibility?
)

data class CourseDto(
    val id: Long,
    val title: String,
    val description: String?,
    val ownerId: Long,
    val visibility: CourseVisibility,
    val moderationStatus: ModerationStatus?,
    val canEdit: Boolean, // для текущего пользователя
    val memberCount: Int,
    val enrolledCount: Int
)

data class CourseEnrollmentDto(
    val id: Long,
    val courseId: Long,
    val userId: Long,
    val enrolledAt: Instant
) {
    companion object {
        fun from(entity: CourseEnrollment) = CourseEnrollmentDto(
            id = entity.id!!,
            courseId = entity.course.id!!,
            userId = entity.user.id,
            enrolledAt = entity.enrolledAt
        )
    }
}

data class CreateStepDto(
    val name: String,
    val content: String,
    val type: StepType
)

data class UpdateStepDto(
    val name: String?,
    val content: String?,
    val type: StepType?
)

data class CourseGraphDto(
    val steps: List<StepDto>,
    val edges: List<EdgeDto>
)

data class StepDto(
    val id: Long,
    val name: String,
    val type: StepType,
    val content: String? // null если шаг ещё недоступен
)

data class EdgeDto(
    val id: Long,
    val fromStepId: Long,
    val toStepId: Long,
    val requiredScore: Int?
)

//data class StepWithProgressDto(
//    val step: StepDto,
//    val status: ProgressStatus, // LOCKED, AVAILABLE, COMPLETED
//    val score: Int?
//)