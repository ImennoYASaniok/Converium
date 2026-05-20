package com.course

import com.course.dto.*

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

// Контроллер курса

@RestController
@RequestMapping("/api/courses")
class CourseController(
    private val courseService: CourseService
) {

    private fun getCurrentUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? Long
                ?: throw IllegalArgumentException("Требуется аутентификация")
    }

    @GetMapping("/my")
    fun getMyCourses(): ResponseEntity<List<CourseDto>> {
        val userId = getCurrentUserId()
        val courses = courseService.getMyCourses(userId)
        return ResponseEntity.ok(courses)
    }

    @PostMapping
    fun createCourse(
        @RequestBody dto: CreateCourseDto,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) userId: Long? = null
    ): ResponseEntity<CourseDto> {
        val ownerId = userId ?: 1L // authentication.name.toLong()
        val course = courseService.createCourse(dto, ownerId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseDto.from(course, true))
    }

    @GetMapping("/{courseId:\\d+}")
    fun getCourse(
        @PathVariable courseId: Long,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<CourseDto> {
        val userId = reqUserId ?: 1L // authentication.name.toLong()
        val courseDto = courseService.getCourseDto(courseId, userId)
        return ResponseEntity.ok(courseDto)
    }

    @DeleteMapping("/{courseId:\\d+}")
    fun deleteCourse(
        @PathVariable courseId: Long,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<Void> {
        val userId = reqUserId ?: 1L // authentication.name.toLong()
        courseService.deleteCourse(courseId, userId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{courseId:\\d+}")
    fun updateCourse(
        @PathVariable courseId: Long,
        @RequestBody dto: UpdateCourseDto,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<CourseDto> {
        val userId = reqUserId ?: 1L // authentication.name.toLong()
        val updatedCourse = courseService.updateCourse(courseId, dto, userId)
        return ResponseEntity.ok(courseService.toDto(updatedCourse, userId))
    }

    @PostMapping("/{courseId:\\d+}/start")
    fun startCourse(
        @PathVariable courseId: Long,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<CourseEnrollmentDto> {
        val userId = reqUserId ?: 1L // authentication.name.toLong()
        val enrollment = courseService.startCourse(courseId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseEnrollmentDto.from(enrollment))
    }

    @PostMapping("/{courseId:\\d+}/members")
    fun addMember(
        @PathVariable courseId: Long,
        @RequestBody dto: MemberDto,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<CourseMembershipDto> {
        val userId = reqUserId ?: 1L // authentication.name.toLong()
        val newMember = courseService.addMember(courseId, dto.userId, dto.ability, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseMembershipDto.from(newMember))
    }

    @PutMapping("/{courseId:\\d+}/members")
    fun updateMemberAbility(
        @PathVariable courseId: Long,
        @RequestBody dto: MemberDto,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<CourseMembershipDto> {
        val updaterId = reqUserId ?: 1L // authentication.name.toLong()
        val updated = courseService.updateMemberAbility(courseId, dto.userId, dto.ability, updaterId)
        return ResponseEntity.ok(CourseMembershipDto.from(updated))
    }

    @DeleteMapping("/{courseId:\\d+}/members/{userId}")
    fun removeMember(
        @PathVariable courseId: Long,
        @PathVariable userId: Long,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<Void> {
        val removerId = reqUserId ?: 1L // authentication.name.toLong()
        courseService.removeMember(courseId, userId, removerId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{courseId:\\d+}/structure")
    fun getCourseStructure(
        @PathVariable courseId: Long,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<CourseGraphDto> {
        val userId = reqUserId ?: 1L // authentication.name.toLong()
        val courseStructureDto = courseService.getCourseGraph(courseId, userId)
        return ResponseEntity.ok(courseStructureDto)
    }

    @GetMapping("/user/{userId}")
    fun getUserCourses(
        @PathVariable userId: Long,
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<List<CourseDto>> {
        val userIdHeader = reqUserId ?: 1L
        val courses = courseService.getCoursesByUser(userId, reqUserId)
        return ResponseEntity.ok(courses)
    }

    @GetMapping("/search")
    fun searchCourses(@RequestParam q: String, @RequestParam(required = false) by: String?, @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null): ResponseEntity<List<CourseDto>> {
        val currentUserId = reqUserId ?: -1L
        val courses = courseService.searchCourses(q, if (currentUserId >= 0) currentUserId else null, by)
        return ResponseEntity.ok(courses)
    }


    @PostMapping("/{courseId:\\d+}/structure")
    fun updateCourseStructure(
        @PathVariable courseId: Long,
        @RequestBody dto: CourseGraphDto,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity.BodyBuilder {
        val userId = reqUserId ?: 1L // authentication.name.toLong()
        courseService.updateCourseGraph(courseId, dto, userId)
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
    }

    // --- Step CRUD ---

        @GetMapping("/{courseId:\\d+}/steps/{stepId}")
        fun getStep(
            @PathVariable courseId: Long,
            @PathVariable stepId: Long
        ): ResponseEntity<StepDetailDto> {
        val userId = getCurrentUserId()
        val stepDto = courseService.getStep(courseId, stepId, userId)
        return ResponseEntity.ok(stepDto)
    }

        @PutMapping("/{courseId:\\d+}/steps/{stepId}")
        fun updateStep(
            @PathVariable courseId: Long,
            @PathVariable stepId: Long,
            @RequestBody dto: UpdateStepDto
        ): ResponseEntity<StepDetailDto> {
        val userId = getCurrentUserId()
        val updated = courseService.updateStep(courseId, stepId, dto, userId)
        return ResponseEntity.ok(updated)
    }

        @DeleteMapping("/{courseId:\\d+}/steps/{stepId}")
        fun deleteStep(
            @PathVariable courseId: Long,
            @PathVariable stepId: Long
        ): ResponseEntity<Void> {
        val userId = getCurrentUserId()
        courseService.deleteStep(courseId, stepId, userId)
        return ResponseEntity.noContent().build()
    }
}
