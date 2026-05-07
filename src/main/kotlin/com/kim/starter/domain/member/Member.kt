package com.kim.starter.domain.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * 회원 도메인 모델 (헥사고날 + 도메인 모델 패턴).
 *
 * - `private constructor` + companion object의 동사형 팩토리(`register`)로만 생성한다(CLAUDE.md §2).
 *   외부에서 `var`를 직접 변경하는 경로를 차단한다.
 * - 상태 전이는 [activate] / [deactivate] / [ban] 등 행위 메서드를 통해서만 일어난다.
 * - JPA 어노테이션은 도메인에 허용(헥사고날 룰의 예외, splearn 스타일).
 *   Spring 어노테이션은 절대 박지 않는다.
 *
 * Email은 String 컬럼으로 영속화하고 [email] getter로 VO를 노출한다 — Hibernate 7의 Kotlin
 * value class `@Embeddable` 지원이 불안정하여 영속 표현은 단순 String으로 둔다.
 */
@Entity
@Table(name = "members")
class Member private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "email", nullable = false, unique = true, length = Email.MAX_LENGTH)
    val emailValue: String,
    @Column(name = "password_hash", nullable = false, length = 255)
    val passwordHash: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private var status: MemberStatus,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime,
    @Column(name = "updated_at", nullable = false)
    private var updatedAt: OffsetDateTime,
) {
    val email: Email get() = Email(emailValue)

    val currentStatus: MemberStatus get() = status

    val updatedAtValue: OffsetDateTime get() = updatedAt

    val isActive: Boolean get() = status == MemberStatus.ACTIVE

    /** PENDING → ACTIVE 전이. 이미 활성/정지/휴면이면 거부. */
    fun activate(now: OffsetDateTime) {
        check(status == MemberStatus.PENDING) { "PENDING 상태에서만 활성화 가능: $status" }
        status = MemberStatus.ACTIVE
        updatedAt = now
    }

    /** ACTIVE → INACTIVE (휴면). 이미 휴면/정지/대기면 거부. */
    fun deactivate(now: OffsetDateTime) {
        check(status == MemberStatus.ACTIVE) { "ACTIVE 상태에서만 휴면 전환 가능: $status" }
        status = MemberStatus.INACTIVE
        updatedAt = now
    }

    /** 정책 위반 등으로 정지. 어떤 상태에서도 호출 가능. */
    fun ban(now: OffsetDateTime) {
        check(status != MemberStatus.BANNED) { "이미 정지 상태입니다" }
        status = MemberStatus.BANNED
        updatedAt = now
    }

    companion object {
        /**
         * 신규 회원을 등록한다.
         *
         * starter kit은 fork 즉시 사용 가능하도록 [MemberStatus.ACTIVE]로 시작한다.
         * 이메일 인증/관리자 승인 단계가 필요한 도메인은 이 팩토리를 PENDING으로 바꾸고
         * 해당 단계가 완료되었을 때 [activate]를 호출하도록 흐름을 갱신한다.
         */
        fun register(
            email: Email,
            passwordHash: String,
            now: OffsetDateTime,
        ): Member =
            Member(
                emailValue = email.value,
                passwordHash = passwordHash,
                status = MemberStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
    }
}
