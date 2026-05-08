package com.kim.starter.application.member

import com.kim.starter.application.member.provided.MemberRegister
import com.kim.starter.application.required.MemberRepository
import com.kim.starter.application.required.MetricRecorder
import com.kim.starter.application.required.MetricRecorder.RegistrationResult
import com.kim.starter.domain.member.DuplicateEmailException
import com.kim.starter.domain.member.Email
import com.kim.starter.domain.member.Member
import com.kim.starter.domain.member.MemberStatus
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@DisplayName("MemberRegistrationService")
class MemberRegistrationServiceTest {
    private val now = Instant.parse("2026-05-07T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val members = mockk<MemberRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val metrics = mockk<MetricRecorder>()
    private val service = MemberRegistrationService(members, passwordEncoder, clock, metrics)

    @Test
    fun `이메일이 중복되지 않으면 해시된 비밀번호로 회원을 저장하고 success 카운터를 기록한다`() {
        val email = Email("user@example.com")
        every { members.existsByEmail(email) } returns false
        every { passwordEncoder.encode("rawPwd") } returns "hashedPwd"
        val saved = slot<Member>()
        every { members.save(capture(saved)) } answers { saved.captured }
        justRun { metrics.recordMemberRegistration(RegistrationResult.SUCCESS) }

        val result =
            service.register(
                MemberRegister.RegisterCommand(email = email, rawPassword = "rawPwd"),
            )

        assertThat(result.email).isEqualTo(email)
        assertThat(result.passwordHash).isEqualTo("hashedPwd")
        assertThat(result.currentStatus).isEqualTo(MemberStatus.ACTIVE)
        verify(exactly = 1) { members.save(any()) }
        verify(exactly = 1) { metrics.recordMemberRegistration(RegistrationResult.SUCCESS) }
    }

    @Test
    fun `이메일이 중복되면 duplicate 카운터를 기록하고 DuplicateEmailException으로 거부한다`() {
        val email = Email("user@example.com")
        every { members.existsByEmail(email) } returns true
        justRun { metrics.recordMemberRegistration(RegistrationResult.DUPLICATE) }

        assertThatThrownBy {
            service.register(MemberRegister.RegisterCommand(email = email, rawPassword = "rawPwd"))
        }.isInstanceOf(DuplicateEmailException::class.java)

        verify(exactly = 0) { members.save(any()) }
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify(exactly = 1) { metrics.recordMemberRegistration(RegistrationResult.DUPLICATE) }
    }
}
