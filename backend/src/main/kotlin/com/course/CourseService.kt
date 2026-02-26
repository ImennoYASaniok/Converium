package com.course

import com.course.dto.*
import com.course.models.*
import com.course.repositories.CourseEnrollmentRepository
import com.course.repositories.CourseMembershipRepository
import com.course.repositories.CourseRepository
import com.course.repositories.StepEdgeRepository
import com.course.repositories.StepRepository
import com.user.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.AccessDeniedException
import java.time.Instant

@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val courseMembershipRepository: CourseMembershipRepository,
    private val courseEnrollmentRepository: CourseEnrollmentRepository,
    private val stepRepository: StepRepository,
    private val stepEdgeRepository: StepEdgeRepository,
    private val userRepository: UserRepository,
) {
    private val logger = LoggerFactory.getLogger(CourseService::class.java)

    @Transactional
    fun createCourse(dto: CreateCourseDto, ownerId: Long): Course {
        logger.info("Создание курса '{}' пользователем {}...", dto.title, ownerId)

        require(dto.title.isNotBlank()) { "Заголовок не может быть пустым" }
        require(dto.title.length <= 200) { "Заголовок слишком длинный (муксимум - 200 символов)" }
        dto.description?.let {
            require(it.length <= 5000) { "Описание слишком длинное (максимум - 5000 символов)" }
        }
        val owner = userRepository.findById(ownerId)
            .orElseThrow { IllegalArgumentException("Пользователь не найден: $ownerId") }


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


        logger.info("Курс создан, id={}", savedCourse.id)
        return savedCourse
    }

    @Transactional(readOnly = true)
    fun getCourseDto(courseId: Long, userId: Long): CourseDto {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }

        if (!canEdit(course, userId) and !canView(course, userId)) {
            throw RuntimeException("Нет прав для просмотра курса")
        }

        return CourseDto(
            id = course.id!!,
            title = course.title,
            description = course.description,
            ownerId = course.owner.id!!,
            visibility = course.visibility,
            moderationStatus = course.moderationStatus,
            canEdit = canEdit(course, userId),
            memberCount = course.memberships.size,
            enrolledCount = course.enrollments.size
        )
    }

    fun updateCourse(courseId: Long, dto: UpdateCourseDto, requesterId: Long): Course {
        logger.info("Обновление курса {} пользователем {}...", courseId, requesterId)

        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }

        if (!canEdit(course, requesterId)) {
            throw RuntimeException("Нет прав на изменение курса")
        }

        dto.title?.let { course.title = it }
        dto.description?.let { course.description = it }

        dto.visibility?.let { newVisibility ->
            if (newVisibility == CourseVisibility.PUBLIC && course.visibility != CourseVisibility.PUBLIC) {
                course.moderationStatus = ModerationStatus.PENDING
                // TODO: отправить уведомление в Telegram на модерацию
            }
            if (course.visibility == CourseVisibility.PUBLIC && newVisibility != CourseVisibility.PUBLIC) {
                course.moderationStatus = null
            }
            course.visibility = newVisibility
        }
        course.updatedAt = Instant.now()

        logger.debug("Обновлённые данные: {}", dto)
        return courseRepository.save(course)
    }

    @Transactional
    fun deleteCourse(courseId: Long, requesterId: Long) {
        logger.warn("Удаление курса {} пользователем {}...", courseId, requesterId)

        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }

        if ((course.owner.id != requesterId) && (course.memberships.find { it.user.id == requesterId }?.ability != UserAbility.ADMIN)) {
            throw RuntimeException("Нет прав на удаление курса")
        }
        courseRepository.delete(course)
        logger.info("Курс {} удалён", courseId)
    }

    @Transactional
    fun startCourse(courseId: Long, userId: Long): CourseEnrollment {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }

        if (!canView(course, userId)) {
            throw RuntimeException("Нет прав для просмотра курса")
        }

        if (course.enrollments.any { it.user.id == userId }) {
            throw IllegalStateException("Уже записан")
        }

        val user = userRepository.findById(userId).orElseThrow()

        val enrollment = CourseEnrollment(
            course = course,
            user = user
        )

        return courseEnrollmentRepository.save(enrollment)
    }

    fun requestPublicModeration(courseId: Long, requesterId: Long) {// отправка на модерацию

    }

    fun updateModerationStatus(courseId: Long, status: ModerationStatus, moderatorId: Long) { // для админов
    }

    fun addMember(courseId: Long, userId: Long, ability: UserAbility, granterId: Long): CourseMembership {
        return TODO("Provide the return value")
    }

    fun removeMember(courseId: Long, userId: Long, removerId: Long) {}

    fun updateMemberAbility(courseId: Long, userId: Long, newAbility: UserAbility, updaterId: Long): CourseMembership {
        return TODO("Provide the return value")
    }


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

    fun toDto(course: Course, userId: Long): CourseDto = CourseDto(
        id = course.id!!,
        title = course.title,
        description = course.description,
        ownerId = course.owner.id!!,
        visibility = course.visibility,
        moderationStatus = course.moderationStatus,
        canEdit = canEdit(course, userId),
        memberCount = course.memberships.size,
        enrolledCount = course.enrollments.size
    )
}
