package com.chats.course_review.models

import com.course.models.Course
import com.user.models.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "course_reviews")
class CourseReview(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    var rating: Int,

    @Column(length = 5000)
    var text: String? = null,

    @Column(nullable = false)
    var timestamp: Instant = Instant.now(),

    @Column(name = "is_updated", nullable = false)
    var isUpdated: Boolean = false
) {
    fun update(rating: Int, text: String?) {
        this.rating = rating
        this.text = text?.trim()
        this.timestamp = Instant.now()
        this.isUpdated = true
    }
}