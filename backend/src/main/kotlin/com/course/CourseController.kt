package com.course

import com.course.dto.*
import com.course.models.Course

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// Контроллер курса

@RestController
@RequestMapping("/api/courses")
class CourseController(
    private val courseService: CourseService
) {

    @PostMapping
    fun createCourse(
        @RequestBody dto: CreateCourseDto,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) userId: Long? = null
    ): ResponseEntity<Course> {
        val ownerId = userId ?: 37L // 1L // authentication.name.toLong()
        val course = courseService.createCourse(dto, ownerId)
        return ResponseEntity.status(HttpStatus.CREATED).body(course)
    }

    @GetMapping("/{courseId}")
    fun getCourse(
        @PathVariable courseId: Long,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<CourseDto> {
        val userId = reqUserId ?: 1L // authentication.name.toLong()
        val courseDto = courseService.getCourseDto(courseId, userId)
        return ResponseEntity.ok(courseDto)
    }

    @DeleteMapping("/{courseId}")
    fun deleteCourse(
        @PathVariable courseId: Long,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<Void> {
        val userId = reqUserId ?: 1L // authentication.name.toLong()
        courseService.deleteCourse(courseId, userId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{courseId}")
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

    @PostMapping("/{courseId}/start")
    fun startCourse(
        @PathVariable courseId: Long,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<CourseEnrollmentDto> {
        val userId = reqUserId ?: 1L // authentication.name.toLong()
        val enrollment = courseService.startCourse(courseId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseEnrollmentDto.from(enrollment))
    }
}

