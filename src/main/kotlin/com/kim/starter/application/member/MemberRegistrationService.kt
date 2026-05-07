package com.kim.starter.application.member

import com.kim.starter.application.member.provided.MemberRegister
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
 * `Member.register`가 도메인 불변식(상태 = ACTIVE, createdAt == updatedAt)을 책임진다.
 * 서비스는 인프라(중복 체크, 해싱, 시점 결정)만 담당.
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
