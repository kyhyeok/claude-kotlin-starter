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
        // Spring Security 7의 encode가 nullable platform type을 반환할 수 있음.
        val passwordHash =
            requireNotNull(passwordEncoder.encode(command.rawPassword)) {
                "PasswordEncoder.encode가 null을 반환했습니다"
            }
        val now = OffsetDateTime.now(clock)
        val member = Member.register(command.email, passwordHash, now)
        return members.save(member)
    }
}
