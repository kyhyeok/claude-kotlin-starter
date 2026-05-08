package com.kim.starter.application.auth.provided

import com.kim.starter.domain.member.Member

/**
 * 회원 등록 Use Case 포트(provided).
 *
 * 단일 행위 인터페이스 — 어댑터는 이 포트만 의존하고 구현체(`MemberRegistrationService`)는 모른다.
 * 이메일 형식·길이 검증은 어댑터의 DTO Bean Validation(`@Email @Size`)이 담당한다.
 */
interface MemberRegister {
    fun register(command: RegisterCommand): Member

    data class RegisterCommand(
        val email: String,
        val rawPassword: String,
    )
}
