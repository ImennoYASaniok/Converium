package com.course

import com.course.dto.*

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
    ): ResponseEntity<CourseDto> {
        val ownerId = userId ?: 1L // authentication.name.toLong()
        val course = courseService.createCourse(dto, ownerId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseDto.from(course, true))
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

    @PostMapping("/{courseId}/members")
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

    @PutMapping("/{courseId}/members")
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

    @DeleteMapping("/{courseId}/members/{userId}")
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

    @GetMapping("/{courseId}/structure")
    fun getCourseStructure(
        @PathVariable courseId: Long,
//        authentication: Authentication
        @RequestHeader("X-User-Id", required = false) reqUserId: Long? = null
    ): ResponseEntity<CourseGraphDto> {
        val userId = reqUserId ?: 1L // authentication.name.toLong()
        val courseStructureDto = courseService.getCourseGraph(courseId, userId)
        return ResponseEntity.ok(courseStructureDto)
    }


    @PostMapping("/{courseId}/structure")
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
}

