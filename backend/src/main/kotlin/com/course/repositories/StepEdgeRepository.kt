package com.course.repositories

import com.course.models.StepEdge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StepEdgeRepository : JpaRepository<StepEdge, Long> {

    fun findByCourseId(courseId: Long): List<StepEdge>

    // Исходящие рёбра
    fun findByFromId(fromStepId: Long): List<StepEdge>

    // Входящие рёбра
    fun findByToId(toStepId: Long): List<StepEdge>

    // Проверка существования
    fun existsByFromIdAndToId(fromStepId: Long, toStepId: Long): Boolean
}