package com.course

import com.course.dto.*
import com.course.models.*
import com.course.repositories.CourseEnrollmentRepository
import com.course.repositories.CourseMembershipRepository
import com.course.repositories.CourseRepository
import com.course.repositories.StepEdgeRepository
import com.course.repositories.StepRepository
import com.user.dtos.UserSummaryDto
import com.user.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

enum class SearchBy {
    TITLE,
    DESCRIPTION,
    OWNER,
    ALL
}

@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val courseMembershipRepository: CourseMembershipRepository,
    private val courseEnrollmentRepository: CourseEnrollmentRepository,
    private val stepRepository: StepRepository,
    private val stepEdgeRepository: StepEdgeRepository,
    private val userRepository: UserRepository
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

        // Даже если PUBLIC, нужно хранить админов и т.д.
        val ownerMembership = CourseMembership(
            course = savedCourse,
            user = owner,
            ability = UserAbility.ADMIN,
            grantedBy = owner
        )
        courseMembershipRepository.save(ownerMembership)


        logger.info("Курс создан, id={}", savedCourse.id)
        return savedCourse
    }

    @Transactional(readOnly = true)
    fun getCourseDto(courseId: Long, userId: Long): CourseDto {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }

        if (!canView(course, userId)) {
            throw RuntimeException("Нет прав для просмотра курса")
        }

        return toDto(course, userId)
    }
    @Transactional
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

    @Transactional
    fun addMember(courseId: Long, userId: Long, ability: UserAbility, granterId: Long): CourseMembership {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }
        if (!canManageMembers(course, granterId)) {
            throw RuntimeException("Нет прав для добавления пользователей")
        }
        if (course.owner.id == userId) {
            throw IllegalStateException("Пользователь уже является участником (владельцем)")
        }
        if (course.memberships.any { it.user.id == userId }) {
            throw IllegalStateException("Пользователь уже является участником")
        }
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("Пользователь для добавления не найден: $userId") }
        val granter = userRepository.findById(granterId)
            .orElseThrow { IllegalArgumentException("Админ не найден: $granterId") }

        val newMember = CourseMembership(
            course = course,
            user =  user,
            ability = ability,
            grantedBy = granter
        )

        return courseMembershipRepository.save(newMember)
    }
    @Transactional
    fun removeMember(courseId: Long, userId: Long, removerId: Long) {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }

        if (!canManageMembers(course, removerId)) {
            throw RuntimeException("Нет прав для удаления участников")
        }

        if (course.owner.id == userId) {
            throw IllegalStateException("Нельзя удалить владельца курса")
        }

        if (userId == removerId) {
            course.memberships.find {
                it.user.id != removerId && it.ability == UserAbility.ADMIN
            }?:throw IllegalStateException("Нельзя удалить себя — вы последний администратор")
        }

        courseMembershipRepository.deleteByCourseIdAndUserId(courseId, userId)
    }

    @Transactional
    fun updateMemberAbility(
        courseId: Long,
        userId: Long,
        newAbility: UserAbility,
        updaterId: Long
    ): CourseMembership {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }

        if (!canManageMembers(course, updaterId)) {
            throw RuntimeException("Нет прав для изменения участников")
        }

        val membership = course.memberships.find { it.user.id == userId }
            ?: throw IllegalArgumentException("Участник не найден в курсе: $userId")

        if (course.owner.id == userId) {
            throw IllegalStateException("Нельзя изменить права владельца курса")
        }

        if (membership.user.id == updaterId && newAbility != UserAbility.ADMIN) {
            if (userId == updaterId) {
                course.memberships.find {
                    it.user.id != updaterId && it.ability == UserAbility.ADMIN
                }?:throw IllegalStateException("Нельзя понизить себя — вы последний администратор")
            }

        }

        membership.ability = newAbility
        return courseMembershipRepository.save(membership)
    }



    fun getMyCourses(userId: Long): List<CourseDto> {
        val owned = courseRepository.findByOwnerId(userId)
        val memberOf = courseRepository.findByMemberUserId(userId)
        val all = (owned + memberOf).distinctBy { it.id }
        return all.map { toDto(it, userId) }
    }

    @Transactional(readOnly = true)
    fun getCoursesByUser(targetUserId: Long, requesterId: Long?): List<CourseDto> {
        val owned = courseRepository.findByOwnerId(targetUserId)
        val memberOf = courseRepository.findByMemberUserId(targetUserId)
        val all = (owned + memberOf).distinctBy { it.id }
        return all.map { toDto(it, requesterId ?: targetUserId) }
    }

    @Transactional(readOnly = true)
    fun searchCourses(query: String, requesterId: Long?, by: SearchBy?): List<CourseDto> {
        val trimmed = query.trim()
        val courses = if (trimmed.isEmpty()) {
            courseRepository.findTop10Courses()
        } else {
            when (by ?: SearchBy.ALL) {
                SearchBy.TITLE -> courseRepository.findByTitleContainingIgnoreCase(trimmed)
                SearchBy.DESCRIPTION -> courseRepository.findByDescriptionContainingIgnoreCase(trimmed)
                SearchBy.OWNER -> courseRepository.findByOwnerLoginContainingIgnoreCase(trimmed)
                SearchBy.ALL -> (courseRepository.findByTitleContainingIgnoreCase(trimmed) + courseRepository.findByDescriptionContainingIgnoreCase(trimmed)).distinctBy { it.id }
            }
        }
        return courses.map { toDto(it, requesterId ?: -1L) }
    }

    @Transactional
    fun getCourseGraph(courseId: Long, requesterId: Long): CourseGraphDto {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }
        if (!canView(course, requesterId)) {
            throw RuntimeException("Нет прав для просмотра курса")
        }

        return CourseGraphDto(
            steps = (course.steps).map { step -> StepDto.from(step) },
            edges = (course.edges).map { edge -> EdgeDto.from(edge) }
        )
    }
    
    @Transactional(readOnly = true)
    fun getStep(courseId: Long, stepId: Long, userId: Long): StepDetailDto {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }
        if (!canView(course, userId)) {
            throw RuntimeException("Нет прав для просмотра курса")
        }
        val step = stepRepository.findById(stepId)
            .orElseThrow { IllegalArgumentException("Шаг не найден: $stepId") }
        if (step.course.id != courseId) {
            throw IllegalArgumentException("Шаг не принадлежит курсу")
        }
        return StepDetailDto.from(step)
    }

    @Transactional
    fun updateStep(courseId: Long, stepId: Long, dto: UpdateStepDto, userId: Long): StepDetailDto {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }
        if (!canEdit(course, userId)) {
            throw RuntimeException("Нет прав на изменение курса")
        }
        val step = stepRepository.findById(stepId)
            .orElseThrow { IllegalArgumentException("Шаг не найден: $stepId") }
        if (step.course.id != courseId) {
            throw IllegalArgumentException("Шаг не принадлежит курсу")
        }
        dto.name?.let { step.name = it }
        dto.description?.let { step.description = it }
        dto.content?.let { step.content = it }
        return StepDetailDto.from(stepRepository.save(step))
    }

    @Transactional
    fun deleteStep(courseId: Long, stepId: Long, userId: Long) {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }
        if (!canEdit(course, userId)) {
            throw RuntimeException("Нет прав на изменение курса")
        }
        val step = stepRepository.findById(stepId)
            .orElseThrow { IllegalArgumentException("Шаг не найден: $stepId") }
        if (step.course.id != courseId) {
            throw IllegalArgumentException("Шаг не принадлежит курсу")
        }
        stepRepository.delete(step)
    }
    @Transactional
    fun updateCourseGraph(courseId: Long, dto: CourseGraphDto, requesterId: Long)  {
        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Курс не найден: $courseId") }

        if (!canEdit(course, requesterId)) {
            throw RuntimeException("Нет прав на изменение курса")
        }

        course.steps.clear()
        dto.steps.forEach { step ->
            step.let { course.steps.add(stepFrom(step, course))}
        }

        course.edges.clear()
        dto.edges.forEach { edge ->
            edge.let { course.edges.add(
                StepEdge(
                    from = stepFrom((dto.steps).first { dto -> dto.id == it.fromStepId }, course),
                    to = stepFrom((dto.steps).first { dto -> dto.id == it.toStepId }, course),
                    course = course,
                    requiredScore = it.requiredScore
                )
            ) }
        }

        course.updatedAt = Instant.now()
        courseRepository.save(course)
    }


    private fun canView(course: Course, userId: Long): Boolean {
        if (userId == course.owner.id) return true
        if (isMember(course, userId)) return true
        return when (course.visibility) {
            CourseVisibility.PUBLIC -> course.moderationStatus == ModerationStatus.APPROVED
            CourseVisibility.FRIENDS_ONLY -> {
                userRepository.areFriends(course.owner.id, userId)
            }
            else -> false
        }
    }

    private fun canEdit(course: Course, userId: Long): Boolean {
        if (course.owner.id == userId) return true
        val membership = course.memberships.find { it.user.id == userId }
        return membership?.ability == UserAbility.EDIT || membership?.ability == UserAbility.ADMIN
    }

    private fun canManageMembers(course: Course, userId: Long): Boolean {
        if (course.owner.id == userId) return true
        val membership = course.memberships.find { it.user.id == userId }
        return membership?.ability == UserAbility.ADMIN
    }

    private fun isMember(course: Course, userId: Long): Boolean {
        return courseMembershipRepository.existsByCourseIdAndUserId(course.id!!, userId)
    }

    fun toDto(course: Course, userId: Long): CourseDto = CourseDto(
        id = course.id!!,
        title = course.title,
        description = course.description,
        owner = UserSummaryDto.from(course.owner),
        visibility = course.visibility,
        moderationStatus = course.moderationStatus,
        canEdit = canEdit(course, userId),
        memberCount = course.memberships.size,
        enrolledCount = course.enrollments.size,
        createdAt = course.createdAt,
        updatedAt = course.updatedAt
    )
    private fun stepFrom(dto: StepDto, course: Course): Step {
        return Step(
            name = dto.name,
            description = dto.description,
            content = dto.content!!,
            course = course,
            type = dto.type
        )
    }
}
