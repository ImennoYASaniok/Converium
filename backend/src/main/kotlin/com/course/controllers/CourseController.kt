package com.course.controllers

import com.course.services.CourseService
import com.course.services.SearchBy
import com.course.dto.*
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/courses")
class CourseController(
    private val courseService: CourseService
) {
    @GetMapping("/my")
    fun getMyCourses(
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<List<CourseDto>> {
        val courses = courseService.getMyCourses(reqUserId)
        return ResponseEntity.ok(courses)
    }

    @PostMapping
    fun createCourse(
        @Valid @RequestBody dto: CreateCourseDto,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<CourseDto> {
        val course = courseService.createCourse(dto, reqUserId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseDto.from(course, true))
    }

    @GetMapping("/search")
    fun searchCourses(
        @RequestParam q: String,
        @RequestParam(required = false) by: String?,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<List<CourseDto>> {
        val searchBy = by?.let { SearchBy.valueOf(it.uppercase()) }
        val courses = courseService.searchCourses(q, reqUserId, searchBy)
        return ResponseEntity.ok(courses)
    }

    @GetMapping("/{courseId}")
    fun getCourse(
        @PathVariable courseId: Long,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<CourseDto> {
        val courseDto = courseService.getCourseDto(courseId, reqUserId)
        return ResponseEntity.ok(courseDto)
    }

    @DeleteMapping("/{courseId}")
    fun deleteCourse(
        @PathVariable courseId: Long,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<Void> {
        courseService.deleteCourse(courseId, reqUserId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{courseId}")
    fun updateCourse(
        @PathVariable courseId: Long,
        @Valid @RequestBody dto: UpdateCourseDto,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<CourseDto> {
        val updatedCourse = courseService.updateCourse(courseId, dto, reqUserId)
        return ResponseEntity.ok(courseService.toDto(updatedCourse, reqUserId))
    }

    @PostMapping("/{courseId}/start")
    fun startCourse(
        @PathVariable courseId: Long,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<CourseEnrollmentDto> {
        val enrollment = courseService.startCourse(courseId, reqUserId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseEnrollmentDto.from(enrollment))
    }

    @PostMapping("/{courseId}/members")
    fun addMember(
        @PathVariable courseId: Long,
        @RequestBody dto: MemberDto,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<CourseMembershipDto> {
        val newMember = courseService.addMember(courseId, dto.userId, dto.ability, reqUserId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseMembershipDto.from(newMember))
    }

    @PutMapping("/{courseId}/members")
    fun updateMemberAbility(
        @PathVariable courseId: Long,
        @RequestBody dto: MemberDto,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<CourseMembershipDto> {
        val updated = courseService.updateMemberAbility(courseId, dto.userId, dto.ability, reqUserId)
        return ResponseEntity.ok(CourseMembershipDto.from(updated))
    }

    @DeleteMapping("/{courseId}/members/{userId}")
    fun removeMember(
        @PathVariable courseId: Long,
        @PathVariable userId: Long,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<Void> {
        courseService.removeMember(courseId, userId, reqUserId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{courseId}/structure")
    fun getCourseStructure(
        @PathVariable courseId: Long,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<CourseGraphDto> {
        val courseStructureDto = courseService.getCourseGraph(courseId, reqUserId)
        return ResponseEntity.ok(courseStructureDto)
    }

    @PostMapping("/{courseId}/structure")
    fun updateCourseStructure(
        @PathVariable courseId: Long,
        @Valid @RequestBody dto: CourseGraphDto,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<Void> {
        courseService.updateCourseGraph(courseId, dto, reqUserId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{courseId}/steps/{stepId}")
    fun getStep(
        @PathVariable courseId: Long,
        @PathVariable stepId: Long,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<StepDetailDto> {
        val stepDto = courseService.getStep(courseId, stepId, reqUserId)
        return ResponseEntity.ok(stepDto)
    }

    @PutMapping("/{courseId}/steps/{stepId}")
    fun updateStep(
        @PathVariable courseId: Long,
        @PathVariable stepId: Long,
        @Valid @RequestBody dto: UpdateStepDto,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<StepDetailDto> {
        val updated = courseService.updateStep(courseId, stepId, dto, reqUserId)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{courseId}/steps/{stepId}")
    fun deleteStep(
        @PathVariable courseId: Long,
        @PathVariable stepId: Long,
        @AuthenticationPrincipal reqUserId: Long
    ): ResponseEntity<Void> {
        courseService.deleteStep(courseId, stepId, reqUserId)
        return ResponseEntity.noContent().build()
    }
}
