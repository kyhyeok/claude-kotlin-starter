package com.kim.starter.domain.member

import com.kim.starter.support.assertion.MemberAssertions
import com.kim.starter.support.fixture.MemberFixture.member
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DisplayName("Member 도메인")
class MemberTest {
    private val now = OffsetDateTime.of(2026, 5, 7, 0, 0, 0, 0, ZoneOffset.UTC)
    private val email = Email("user@example.com")

    @Test
    fun `register는 ACTIVE 상태와 동일한 createdAt updatedAt으로 회원을 만든다`() {
        val created = member(email = email, passwordHash = "hashed", now = now)

        assertThat(created)
            .satisfies(
                MemberAssertions.isActiveWith(email),
                MemberAssertions.hasTimestamps(createdAt = now),
            )
        assertThat(created.passwordHash).isEqualTo("hashed")
    }

    @Test
    fun `이미 ACTIVE 회원에게 activate를 호출하면 거부한다`() {
        val active = member(email = email, now = now)

        assertThatThrownBy { active.activate(now.plusMinutes(1)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("PENDING 상태에서만")
    }

    @Test
    fun `ACTIVE 회원은 deactivate로 INACTIVE 전이가 가능하다`() {
        val active = member(email = email, now = now)
        val later = now.plusDays(30)

        active.deactivate(later)

        assertThat(active)
            .satisfies(
                MemberAssertions.isInStatus(MemberStatus.INACTIVE),
                MemberAssertions.hasTimestamps(createdAt = now, updatedAt = later),
            )
    }

    @Test
    fun `ban은 어떤 상태에서도 BANNED로 전이한다`() {
        val active = member(email = email, now = now)
        val later = now.plusHours(1)

        active.ban(later)

        assertThat(active)
            .satisfies(
                MemberAssertions.isInStatus(MemberStatus.BANNED),
                MemberAssertions.hasTimestamps(createdAt = now, updatedAt = later),
            )
    }

    @Test
    fun `이미 BANNED 상태에서 ban을 다시 호출하면 거부한다`() {
        val banned = member(email = email, now = now).also { it.ban(now) }

        assertThatThrownBy { banned.ban(now.plusMinutes(1)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("이미 정지")
    }
}
