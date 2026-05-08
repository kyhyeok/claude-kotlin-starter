package com.kim.starter.support.fixture

import com.kim.starter.domain.member.Email
import com.kim.starter.domain.member.Member
import com.kim.starter.domain.member.MemberStatus
import com.kim.starter.support.generator.EmailGenerator.generateEmail
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Member 도메인 객체 fixture (CLAUDE.md §5).
 *
 * - 매 테스트가 독립 데이터를 사용하도록 default 이메일은 random unique.
 * - 특정 필드만 명시하고 싶을 때는 named argument로 overload 호출.
 * - 상태 전이는 도메인 행위 메서드(`deactivate`/`ban`)를 통해서만 만든다 — 캡슐화 우회 금지(§2).
 *   PENDING은 `Member.register`가 ACTIVE로 시작하므로 starter scope에서 fixture로 만들지 않는다.
 *
 * 새 도메인이 추가되면 같은 패턴으로 `OrderFixture` / `ProductFixture` 등을 추가한다.
 */
object MemberFixture {
    private val DEFAULT_NOW: OffsetDateTime
        get() = OffsetDateTime.of(2026, 5, 7, 0, 0, 0, 0, ZoneOffset.UTC)

    fun member(
        email: Email = Email(generateEmail()),
        passwordHash: String = "hashed-pwd",
        now: OffsetDateTime = DEFAULT_NOW,
    ): Member = Member.register(email, passwordHash, now)

    fun memberInStatus(
        status: MemberStatus,
        email: Email = Email(generateEmail()),
        now: OffsetDateTime = DEFAULT_NOW,
    ): Member =
        member(email = email, now = now).also {
            when (status) {
                MemberStatus.ACTIVE -> Unit
                MemberStatus.INACTIVE -> it.deactivate(now)
                MemberStatus.BANNED -> it.ban(now)
                MemberStatus.PENDING ->
                    error(
                        "starter scope의 Member.register는 ACTIVE로만 시작한다. PENDING 시나리오가 필요해지면 " +
                            "도메인 진화로 register 팩토리에 status 파라미터 추가 후 fixture 보강.",
                    )
            }
        }
}
