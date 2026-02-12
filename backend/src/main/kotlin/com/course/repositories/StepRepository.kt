package com.course.repositories


import com.course.models.Step
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StepRepository : JpaRepository<Step, Long> {

    fun findByCourseId(courseId: Long): List<Step>

    fun findByCourseIdOrderById(courseId: Long): List<Step>
}