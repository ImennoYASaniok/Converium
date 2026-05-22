package com.course.dto

import com.course.models.Course
import com.course.models.CourseEnrollment
import com.course.models.CourseMembership
import com.course.models.CourseVisibility
import com.course.models.ModerationStatus
import com.course.models.Step
import com.course.models.StepEdge
import com.course.models.StepType
import com.course.models.UserAbility
import com.user.dtos.UserSummaryDto
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import kotlin.String

data class CreateCourseDto(
    @field:NotBlank(message = "Заголовок не может быть пустым")
    @field:Size(max = 200, message = "Заголовок слишком длинный (максимум - 200 символов)")
    val title: String,
    @field:Size(max = 5000, message = "Описание слишком длинное (максимум - 5000 символов)")
    val description: String?,
    val visibility: CourseVisibility
)


data class UpdateCourseDto(
    @field:Pattern(regexp = "^$|\\s*\\S.*", message = "Заголовок не может быть пустым")
    @field:Size(max = 200, message = "Заголовок слишком длинный (максимум - 200 символов)")
    val title: String?,
    @field:Size(max = 5000, message = "Описание слишком длинное (максимум - 5000 символов)")
    val description: String?,
    val visibility: CourseVisibility?
)

data class CourseDto(
    val id: Long,
    val title: String,
    val description: String?,
    val owner: UserSummaryDto,
    val visibility: CourseVisibility,
    val moderationStatus: ModerationStatus?,
    val canEdit: Boolean, // для текущего пользователя
    val memberCount: Int,
    val enrolledCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(entity: Course, canEdit: Boolean): CourseDto {
            return CourseDto(
                id = entity.id!!,
                title = entity.title,
                description = entity.description,
                owner = UserSummaryDto.from(entity.owner),
                visibility = entity.visibility,
                moderationStatus = entity.moderationStatus,
                canEdit = canEdit,
                memberCount = entity.memberships.size,
                enrolledCount = entity.enrollments.size,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

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

data class CourseGraphDto(
    @field:Valid
    val steps: List<StepDto>,
    @field:Valid
    val edges: List<EdgeDto>
)

data class StepDto(
    val id: Long,
    @field:NotBlank(message = "Название шага не может быть пустым")
    @field:Size(max = 255, message = "Слишком длинное название шага (максимум - 255 символов)")
    val name: String,
    val type: StepType,
    @field:Size(max = 20_000, message = "Слишком длинный контент (содержимое) шага (максимум - 20000 символов)")
    val content: String?, // null если шаг ещё недоступен
    @field:Size(max = 1000, message = "Слишком длинное описание шага (максимум - 1000 символов)")
    val description: String?
) {
    companion object {
        fun from(entity: Step) = StepDto(
            id = entity.id!!,
            name = entity.name,
            type = entity.type,
            description = entity.description,
            content = entity.content
        )
    }
}

data class StepDetailDto(
    val id: Long,
    val name: String,
    val type: StepType,
    val content: String?,
    val description: String?,
    val courseId: Long
) {
    companion object {
        fun from(entity: Step): StepDetailDto {
            return StepDetailDto(
                id = entity.id!!,
                name = entity.name,
                type = entity.type,
                content = entity.content,
                description = entity.description,
                courseId = entity.course.id!!
            )
        }
    }
}

data class UpdateStepDto(
    @field:Pattern(regexp = "^$|\\s*\\S.*", message = "Название шага не может быть пустым")
    @field:Size(max = 255, message = "Слишком длинное название шага (максимум - 255 символов)")
    val name: String?,
    @field:Size(max = 1000, message = "Слишком длинное описание шага (максимум - 1000 символов)")
    val description: String?,
    @field:Size(max = 20_000, message = "Слишком длинный контент (содержимое) шага (максимум - 20000 символов)")
    val content: String?
)

data class CreateStepDto(
    val name: String,
    val type: StepType?,
    val description: String?,
    val content: String?
)

data class EdgeDto(
    val id: Long,
    val fromStepId: Long,
    val toStepId: Long,
    @field:Min(0)
    val requiredScore: Int?
) {
    companion object {
        fun from(entity: StepEdge) = EdgeDto(
            id = entity.id!!,
            fromStepId = entity.from.id!!,
            toStepId = entity.to.id!!,
            requiredScore = entity.requiredScore
        )
    }
}

data class MemberDto(
    val userId: Long,
    val ability: UserAbility
)

data class CourseMembershipDto(
    val id: Long,
    val userId: Long,
    val ability: UserAbility,
    val grantedById: Long
) {
    companion object {
        fun from(entity: CourseMembership) = CourseMembershipDto(
            id = entity.id!!,
            userId = entity.user.id,
            ability = entity.ability,
            grantedById = entity.grantedBy?.id ?: entity.course.owner.id
        )
    }
}

//data class StepWithProgressDto(
//    val step: StepDto,
//    val status: ProgressStatus, // LOCKED, AVAILABLE, COMPLETED
//    val score: Int?
//)