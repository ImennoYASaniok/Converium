package com.course.models

import jakarta.persistence.*

@Entity
@Table(
    name = "step_edges",
    uniqueConstraints = [UniqueConstraint(columnNames = ["from_step_id", "to_step_id"])]
)
data class StepEdge(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // Откуда
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_step_id", nullable = false)
    val from: Step,

    // Куда
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_step_id", nullable = false)
    val to: Step,

    // Текст оповещения, если не выполнено условие для перехода (например, "Необходимо пройти тест на 80+%"). Пока не будем добавлять.
//    @Column(name = "condition_description")
//    val condition: String? = null,

    // Минимальный скор для перехода (null = без условий)
    // Скор можно набирать, например, проходя тесты на высокие проценты
    @Column(name = "required_score")
    val requiredScore: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course
)