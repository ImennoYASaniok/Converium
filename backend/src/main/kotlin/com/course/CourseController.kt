package com.course

import com.course.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

// Контроллер курса

@RestController
@RequestMapping("/api/courses")
class CourseController(private val courseService: CourseService) {
    private fun getCurrentUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? Long
                ?: throw IllegalArgumentException("Требуется аутентификация")
    }

    @PostMapping
    fun createCourse(@RequestBody dto: CreateCourseDto): ResponseEntity<CourseDto> {
        val ownerId = getCurrentUserId()
        val course = courseService.createCourse(dto, ownerId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseDto.from(course, true))
    }

    @GetMapping("/{courseId}")
    fun getCourse(@PathVariable courseId: Long): ResponseEntity<CourseDto> {
        val userId = getCurrentUserId()
        val courseDto = courseService.getCourseDto(courseId, userId)
        return ResponseEntity.ok(courseDto)
    }

    @DeleteMapping("/{courseId}")
    fun deleteCourse(@PathVariable courseId: Long): ResponseEntity<Void> {
        val userId = getCurrentUserId()
        courseService.deleteCourse(courseId, userId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{courseId}")
    fun updateCourse(
            @PathVariable courseId: Long,
            @RequestBody dto: UpdateCourseDto
    ): ResponseEntity<CourseDto> {
        val userId = getCurrentUserId()
        val updatedCourse = courseService.updateCourse(courseId, dto, userId)
        return ResponseEntity.ok(courseService.toDto(updatedCourse, userId))
    }

    @PostMapping("/{courseId}/start")
    fun startCourse(@PathVariable courseId: Long): ResponseEntity<CourseEnrollmentDto> {
        val userId = getCurrentUserId()
        val enrollment = courseService.startCourse(courseId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseEnrollmentDto.from(enrollment))
    }

    @PostMapping("/{courseId}/members")
    fun addMember(
            @PathVariable courseId: Long,
            @RequestBody dto: MemberDto
    ): ResponseEntity<CourseMembershipDto> {
        val userId = getCurrentUserId()
        val newMember = courseService.addMember(courseId, dto.userId, dto.ability, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseMembershipDto.from(newMember))
    }

    @PutMapping("/{courseId}/members")
    fun updateMemberAbility(
            @PathVariable courseId: Long,
            @RequestBody dto: MemberDto
    ): ResponseEntity<CourseMembershipDto> {
        val updaterId = getCurrentUserId()
        val updated =
                courseService.updateMemberAbility(courseId, dto.userId, dto.ability, updaterId)
        return ResponseEntity.ok(CourseMembershipDto.from(updated))
    }

    @DeleteMapping("/{courseId}/members/{userId}")
    fun removeMember(
            @PathVariable courseId: Long,
            @PathVariable userId: Long
    ): ResponseEntity<Void> {
        val removerId = getCurrentUserId()
        courseService.removeMember(courseId, userId, removerId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{courseId}/structure")
    fun getCourseStructure(@PathVariable courseId: Long): ResponseEntity<CourseGraphDto> {
        val userId = getCurrentUserId()
        val courseStructureDto = courseService.getCourseGraph(courseId, userId)
        return ResponseEntity.ok(courseStructureDto)
    }

    @PostMapping("/{courseId}/structure")
    fun updateCourseStructure(
            @PathVariable courseId: Long,
            @RequestBody dto: CourseGraphDto
    ): ResponseEntity.BodyBuilder {
        val userId = getCurrentUserId()
        courseService.updateCourseGraph(courseId, dto, userId)
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
    }
}
