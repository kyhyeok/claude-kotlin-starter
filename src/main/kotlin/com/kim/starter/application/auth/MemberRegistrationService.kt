package com.kim.starter.application.auth

import com.kim.starter.application.auth.provided.MemberRegister
import com.kim.starter.application.required.MemberRepository
import com.kim.starter.domain.member.DuplicateEmailException
import com.kim.starter.domain.member.Member
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime

/**
 * [MemberRegister] 구현 — 이메일 중복 검증 + 비밀번호 해싱 + 도메인 팩토리 호출.
 *
 * Auth 슬라이스의 register 흐름. 서비스는 인프라(중복 체크, 해싱, 시점 결정)만 담당하고
 * 도메인 불변식은 [Member.register]가 책임진다(ADR-0018: starter scope의 단일 Auth 슬라이스).
 *
 * 도메인 메트릭(`member.registration` 카운터 등)은 starter scope 외 — fork된 서비스가
 * Micrometer `MeterRegistry`를 직접 의존하거나 자체 `MetricRecorder` 포트로 추가한다.
 */
@Service
@Transactional
class MemberRegistrationService(
    private val members: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
) : MemberRegister {
    override fun register(command: MemberRegister.RegisterCommand): Member {
        if (members.existsByEmail(command.email)) {
            throw DuplicateEmailException(command.email)
        }
        val passwordHash =
            requireNotNull(passwordEncoder.encode(command.rawPassword)) {
                "PasswordEncoder.encode가 null을 반환했습니다"
            }
        val now = OffsetDateTime.now(clock)
        val member = Member.register(command.email, passwordHash, now)
        return members.save(member)
    }
}
