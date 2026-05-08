package com.kim.starter.support.fixture

import com.kim.starter.domain.member.Member
import com.kim.starter.support.generator.EmailGenerator.generateEmail
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Member 도메인 객체 fixture (CLAUDE.md §5).
 *
 * - 매 테스트가 독립 데이터를 사용하도록 default 이메일은 random unique.
 * - 특정 필드만 명시하고 싶을 때는 named argument로 overload 호출.
 *
 * starter scope의 thin user 모델 — 활성/정지 상태 같은 깊이는 fork된 서비스가 도메인을
 * 확장하면서 fixture에도 함께 추가한다(ADR-0018).
 */
object MemberFixture {
    private val DEFAULT_NOW: OffsetDateTime
        get() = OffsetDateTime.of(2026, 5, 7, 0, 0, 0, 0, ZoneOffset.UTC)

    fun member(
        email: String = generateEmail(),
        passwordHash: String = "hashed-pwd",
        now: OffsetDateTime = DEFAULT_NOW,
    ): Member = Member.register(email, passwordHash, now)
}
