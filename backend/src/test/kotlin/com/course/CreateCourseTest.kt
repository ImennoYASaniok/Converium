package com.course

import com.course.dto.CreateCourseDto
import com.course.models.*
import com.course.repositories.CourseEnrollmentRepository
import com.course.repositories.CourseMembershipRepository
import com.course.repositories.CourseRepository
import com.course.repositories.StepEdgeRepository
import com.course.repositories.StepRepository
import com.user.models.User
import com.user.repositories.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class CreateCourseTest {

    private lateinit var courseRepository: CourseRepository
    private lateinit var courseMembershipRepository: CourseMembershipRepository
    private lateinit var courseEnrollmentRepository: CourseEnrollmentRepository
    private lateinit var stepRepository: StepRepository
    private lateinit var stepEdgeRepository: StepEdgeRepository
    private lateinit var userRepository: UserRepository
    private lateinit var courseService: CourseService


    @BeforeEach
    fun setUp() {
        courseRepository = mockk()
        courseMembershipRepository = mockk()
        courseEnrollmentRepository = mockk()
        stepRepository = mockk()
        stepEdgeRepository = mockk()
        userRepository = mockk()
        courseService = CourseService(
            courseRepository,
            courseMembershipRepository,
            courseEnrollmentRepository,
            stepRepository,
            stepEdgeRepository,
            userRepository
        )
    }

    @Test
    fun `should create course with FRIENDS_ONLY visibility`() {
        val ownerId = 1L
        val owner = User(
            id = ownerId,
            login = "test",
            password = "pass",
            email = "test@test.com",
            name = "Test",
            surname = "User"
        )
        val dto = CreateCourseDto(
            title = "Kotlin Basics",
            description = "Learn Kotlin",
            visibility = CourseVisibility.FRIENDS_ONLY
        )

        val savedCourse = Course(
            id = 1L,
            title = dto.title,
            description = dto.description,
            owner = owner,
            visibility = dto.visibility,
            moderationStatus = null
        )

        every { userRepository.findById(ownerId) } returns Optional.of(owner)
        every { courseRepository.save(any()) } returns savedCourse
        every { courseMembershipRepository.save(any()) } returns mockk()


        val result = courseService.createCourse(dto, ownerId)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.title).isEqualTo("Kotlin Basics")
        assertThat(result.visibility).isEqualTo(CourseVisibility.FRIENDS_ONLY)
        assertThat(result.moderationStatus).isNull()
        assertThat(result.owner).isEqualTo(owner)

        verify { userRepository.findById(ownerId) }
        verify { courseRepository.save(any()) }
        verify { courseMembershipRepository.save(any()) }
    }

    @Test
    fun `createCourse should set PENDING status for PUBLIC course`() {
        val ownerId = 1L
        val owner = User(
            id = ownerId,
            login = "test",
            password = "pass",
            email = "test@test.com",
            name = "Test",
            surname = "User"
        )
        val dto = CreateCourseDto(
            title = "Public Course",
            description = "For everyone",
            visibility = CourseVisibility.PUBLIC
        )

        val savedCourse = Course(
            id = 2L,
            title = dto.title,
            description = dto.description,
            owner = owner,
            visibility = dto.visibility,
            moderationStatus = ModerationStatus.PENDING
        )

        every { userRepository.findById(ownerId) } returns Optional.of(owner)
        every { courseRepository.save(any()) } returns savedCourse


        val result = courseService.createCourse(dto, ownerId)

        assertThat(result.visibility).isEqualTo(CourseVisibility.PUBLIC)
        assertThat(result.moderationStatus).isEqualTo(ModerationStatus.PENDING)

        verify(exactly = 0) { courseMembershipRepository.save(any()) }
    }

    @Test
    fun `should throw when user not found`() {
        val ownerId = 999L
        val dto = CreateCourseDto(
            title = "Test",
            description = "Test",
            visibility = CourseVisibility.FRIENDS_ONLY
        )

        every { userRepository.findById(ownerId) } returns Optional.empty()

        assertThatThrownBy {
            courseService.createCourse(dto, ownerId)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Пользователь не найден")

        verify { userRepository.findById(ownerId) }
        verify(exactly = 0) { courseRepository.save(any()) }
    }

    @Test
    fun `should throw when title is blank`() {
        val ownerId = 1L
        val dto = CreateCourseDto(
            title = "     ",
            description = "Test",
            visibility = CourseVisibility.FRIENDS_ONLY
        )
        assertThatThrownBy {
            courseService.createCourse(dto, ownerId)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Заголовок не может быть пустым")
    }

    @Test
    fun `should trim title and description`() {
        val ownerId = 1L
        val owner = User(
            id = ownerId,
            login = "test",
            password = "pass",
            email = "test@test.com",
            name = "Test",
            surname = "User"
        )
        val dto = CreateCourseDto(
            title = "  Kotlin Course  ",
            description = "  Description  ",
            visibility = CourseVisibility.FRIENDS_ONLY
        )

        every { userRepository.findById(ownerId) } returns Optional.of(owner)
        every { courseRepository.save(any()) } answers { firstArg() }
        every { courseMembershipRepository.save(any()) } returns mockk()


        val result = courseService.createCourse(dto, ownerId)

        assertThat(result.title).isEqualTo("Kotlin Course")
        assertThat(result.description).isEqualTo("Description")
    }
}