package com.course.models

import jakarta.persistence.*

@Entity
@Table(name = "steps")
data class Step(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    // Тип шага (теория, тест, практика)
    @Enumerated(EnumType.STRING)
    val type: StepType = StepType.THEORY
)
