package com.kim.starter.application.auth.provided

import com.kim.starter.application.auth.TokenPair
import com.kim.starter.domain.member.Email

/**
 * 로그인 Use Case 포트(provided). 이메일 + 비밀번호 → Access/Refresh Token.
 *
 * 인증 실패 사유(없는 이메일 / 비밀번호 불일치)는 클라이언트에 구분되지 않는 단일 예외로 통합한다 →
 * 사용자 존재 여부 leak 방지.
 */
interface LoginAuthenticator {
    fun login(command: LoginCommand): TokenPair

    data class LoginCommand(
        val email: Email,
        val rawPassword: String,
    )
}
