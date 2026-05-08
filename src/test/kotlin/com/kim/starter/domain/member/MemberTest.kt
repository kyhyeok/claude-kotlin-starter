package com.kim.starter.domain.member

import com.kim.starter.support.assertion.MemberAssertions
import com.kim.starter.support.fixture.MemberFixture.member
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DisplayName("Member 도메인")
class MemberTest {
    private val now = OffsetDateTime.of(2026, 5, 7, 0, 0, 0, 0, ZoneOffset.UTC)
    private val email = "user@example.com"

    @Test
    fun `register는 활성 상태와 동일한 createdAt updatedAt으로 회원을 만든다`() {
        val created = member(email = email, passwordHash = "hashed", now = now)

        assertThat(created)
            .satisfies(
                MemberAssertions.hasEmail(email),
                MemberAssertions.isActive(),
                MemberAssertions.hasTimestamps(createdAt = now),
            )
        assertThat(created.passwordHash).isEqualTo("hashed")
    }
}
