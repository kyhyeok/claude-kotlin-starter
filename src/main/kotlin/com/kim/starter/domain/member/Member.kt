package com.kim.starter.domain.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

// starter scope의 최소 user 모델 (ADR-0018). 상태 enum/행위 메서드/VO는 fork된 서비스가 추가.
@Entity
@Table(name = "members")
class Member private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "email", nullable = false, unique = true, length = 255)
    val email: String,
    @Column(name = "password_hash", nullable = false, length = 255)
    val passwordHash: String,
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: OffsetDateTime,
) {
    companion object {
        fun register(
            email: String,
            passwordHash: String,
            now: OffsetDateTime,
        ): Member =
            Member(
                email = email,
                passwordHash = passwordHash,
                createdAt = now,
                updatedAt = now,
            )
    }
}
