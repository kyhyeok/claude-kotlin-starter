package com.kim.starter.domain.member

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
        val member = Member.register(email, passwordHash = "hashed", now = now)

        assertThat(member.email).isEqualTo(email)
        assertThat(member.passwordHash).isEqualTo("hashed")
        assertThat(member.currentStatus).isEqualTo(MemberStatus.ACTIVE)
        assertThat(member.isActive).isTrue()
        assertThat(member.createdAt).isEqualTo(now)
        assertThat(member.updatedAtValue).isEqualTo(now)
    }

    @Test
    fun `이미 ACTIVE 회원에게 activate를 호출하면 거부한다`() {
        val member = Member.register(email, "hashed", now)

        assertThatThrownBy { member.activate(now.plusMinutes(1)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("PENDING 상태에서만")
    }

    @Test
    fun `ACTIVE 회원은 deactivate로 INACTIVE 전이가 가능하다`() {
        val member = Member.register(email, "hashed", now)
        val later = now.plusDays(30)

        member.deactivate(later)

        assertThat(member.currentStatus).isEqualTo(MemberStatus.INACTIVE)
        assertThat(member.isActive).isFalse()
        assertThat(member.updatedAtValue).isEqualTo(later)
    }

    @Test
    fun `ban은 어떤 상태에서도 BANNED로 전이한다`() {
        val member = Member.register(email, "hashed", now)
        val later = now.plusHours(1)

        member.ban(later)

        assertThat(member.currentStatus).isEqualTo(MemberStatus.BANNED)
        assertThat(member.updatedAtValue).isEqualTo(later)
    }

    @Test
    fun `이미 BANNED 상태에서 ban을 다시 호출하면 거부한다`() {
        val member = Member.register(email, "hashed", now)
        member.ban(now)

        assertThatThrownBy { member.ban(now.plusMinutes(1)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("이미 정지")
    }
}
