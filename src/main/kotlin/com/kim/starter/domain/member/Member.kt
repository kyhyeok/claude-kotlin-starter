package com.kim.starter.domain.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * 회원 도메인 모델 (starter scope의 최소 user 모델 — ADR-0018).
 *
 * 인증/Auth가 요구하는 최소 필드만 박는다: id, email, passwordHash, isActive, 시점.
 * 상태 enum, 행위 메서드(activate/deactivate/ban), Email VO 등 도메인 깊이는
 * fork된 서비스가 도메인에 추가한다.
 *
 * - `private constructor` + companion object의 동사형 팩토리(`register`)로만 생성한다(CLAUDE.md §2).
 * - `isActive`는 default true. starter scope에서는 활성/비활성 분기 시연을 하지 않는다 —
 *   비활성 회원 로그인 차단이 필요한 fork는 application/auth에서 분기를 추가한다.
 * - JPA 어노테이션은 도메인에 허용(헥사고날 룰의 예외, splearn 스타일).
 *   Spring 어노테이션은 절대 박지 않는다.
 */
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
        /**
         * 신규 회원을 등록한다. 시점은 호출자가 [java.time.Clock]으로 결정한다(CLAUDE.md §4).
         */
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
