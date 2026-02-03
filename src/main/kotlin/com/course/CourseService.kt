package com.course

import com.course.dto.*
import com.course.models.*
import com.course.repositories.CourseMembershipRepository
import com.course.repositories.CourseRepository
import com.course.repositories.StepEdgeRepository
import com.course.repositories.StepRepository
import com.user.repositories.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.AccessDeniedException

@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val courseMembershipRepository: CourseMembershipRepository,
    private val stepRepository: StepRepository,
    private val stepEdgeRepository: StepEdgeRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createCourse(dto: CreateCourseDto, ownerId: Long): Course {
        require(dto.title.isNotBlank()) { "Title cannot be blank" }
        require(dto.title.length <= 200) { "Title too long (max 200 chars)" }
        dto.description?.let {
            require(it.length <= 5000) { "Description too long (max 5000 chars)" }
        }
        val owner = userRepository.findById(ownerId)
            .orElseThrow { IllegalArgumentException("User not found: $ownerId") }


        val moderationStatus = when (dto.visibility) {
            CourseVisibility.PUBLIC -> ModerationStatus.PENDING
            else -> null
        }

        val course = Course(
            title = dto.title.trim(),
            description = dto.description?.trim(),
            owner = owner,
            visibility = dto.visibility,
            moderationStatus = moderationStatus
        )

        val savedCourse = courseRepository.save(course)
        val ownerMembership = CourseMembership(
            course = savedCourse,
            user = owner,
            ability = UserAbility.ADMIN,
            grantedBy = owner
        )
        courseMembershipRepository.save(ownerMembership)


        // TODO: если PUBLIC — отправить уведомление в Telegram на модерацию



        return savedCourse
    }

    @Transactional(readOnly = true)
    fun getCourseDto(courseId: Long, userId: Long): CourseDto {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }

        if (!canView(course, userId)) {
            throw RuntimeException("Access denied")
        }

        return CourseDto(
            id = course.id!!,
            title = course.title,
            description = course.description,
            ownerId = course.owner.id!!,
            visibility = course.visibility,
            moderationStatus = course.moderationStatus,
            canEdit = canEdit(course, userId),
            memberCount = course.memberships.size
        )
    }

    fun updateCourse(courseId: Long, dto: UpdateCourseDto, requesterId: Long): Course {
        return TODO("Provide the return value")
    }

    @Transactional
    fun deleteCourse(courseId: Long, requesterId: Long) {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }

        if ((course.owner.id != requesterId) && (course.memberships.find { it.user.id == requesterId }?.ability != UserAbility.ADMIN)) {
            throw RuntimeException("Access denied: cannot delete course")
        }
        courseRepository.delete(course)
    }

    fun requestPublicModeration(courseId: Long, requesterId: Long) {// отправка на модерацию
    }

    fun updateModerationStatus(courseId: Long, status: ModerationStatus, moderatorId: Long) { // для админов
    }
    // === Управление участниками (для CERTAIN_PEOPLE) ===

    fun addMember(courseId: Long, userId: Long, ability: UserAbility, granterId: Long): CourseMembership {
        return TODO("Provide the return value")
    }

    fun removeMember(courseId: Long, userId: Long, removerId: Long) {}

    fun updateMemberAbility(courseId: Long, userId: Long, newAbility: UserAbility, updaterId: Long): CourseMembership {
        return TODO("Provide the return value")
    }

    // === Управление графом курса ===

    fun addStep(courseId: Long, dto: CreateStepDto, requesterId: Long): Step {
        return TODO("Provide the return value")
    }

    fun updateStep(stepId: Long, dto: UpdateStepDto, requesterId: Long): Step {
        return TODO("Provide the return value")
    }

    fun removeStep(stepId: Long, requesterId: Long) {}

    fun addEdge(courseId: Long, fromStepId: Long, toStepId: Long, requiredScore: Int?, requesterId: Long): StepEdge {
        return TODO("Provide the return value")
    }

    fun removeEdge(edgeId: Long, requesterId: Long) {}

    fun getCourseGraph(courseId: Long, requesterId: Long): CourseGraphDto { // шаги + рёбра
        return TODO("Provide the return value")
    }

    // === Навигация по курсу ===

    fun getAvailableSteps(courseId: Long, userId: Long): List<Step> {// какие шаги доступны сейчас
        return TODO("Provide the return value")
    }

//    fun getStepWithProgress(stepId: Long, userId: Long): StepWithProgressDto {
//        return TODO("Provide the return value")
//    }


    private fun canView(course: Course, userId: Long): Boolean {
        return when (course.visibility) {
            CourseVisibility.PUBLIC -> course.moderationStatus == ModerationStatus.APPROVED
            CourseVisibility.FRIENDS_ONLY -> {
                val isOwner = course.owner.id == userId
                if (isOwner) return true

                val isFriend = userRepository.areFriends(course.owner.id!!, userId)
                isFriend || isMember(course, userId)
            }
            CourseVisibility.CERTAIN_PEOPLE -> isMember(course, userId)
        }
    }

    private fun canEdit(course: Course, userId: Long): Boolean {
        if (course.owner.id == userId) return true
        val membership = course.memberships.find { it.user.id == userId }
        return membership?.ability == UserAbility.EDIT || membership?.ability == UserAbility.ADMIN
    }

    private fun canManageMembers(course: Course, userId: Long): Boolean {
        return TODO("Provide the return value")
    }

    private fun isMember(course: Course, userId: Long): Boolean {
        return courseMembershipRepository.existsByCourseIdAndUserId(course.id!!, userId)
    }

    private fun getUserAbility(course: Course, userId: Long): UserAbility? {
        return TODO("Provide the return value")
    }
}