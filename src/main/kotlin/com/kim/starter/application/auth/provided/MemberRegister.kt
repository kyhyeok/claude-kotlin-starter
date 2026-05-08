package com.kim.starter.application.member.provided

import com.kim.starter.domain.member.Email
import com.kim.starter.domain.member.Member

/**
 * 회원 등록 Use Case 포트(provided).
 *
 * 단일 행위 인터페이스 — 어댑터는 이 포트만 의존하고 구현체(`MemberRegistrationService`)는 모른다.
 */
interface MemberRegister {
    fun register(command: RegisterCommand): Member

    data class RegisterCommand(
        val email: Email,
        val rawPassword: String,
    )
}
