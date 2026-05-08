package com.kim.starter.adapter.webapi.auth

import com.kim.starter.application.auth.TokenPair
import com.kim.starter.domain.member.Member
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

/**
 * 인증 API 요청/응답 DTO.
 *
 * - 이메일 형식·길이 검증은 Bean Validation(`@Email @Size(max=255)`)이 담당한다 — 도메인은
 *   String email만 보유하고 검증 책임을 갖지 않는다(ADR-0018).
 * - 응답은 OAuth 2.0 토큰 응답 컨벤션을 따른다(tokenType / expiresIn-seconds).
 */
data class RegisterRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    val password: String,
)

data class LoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    val password: String,
)

data class RefreshRequest(
    @field:NotBlank
    val refreshToken: String,
)

data class MemberResponse(
    val id: Long,
    val email: String,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
) {
    companion object {
        fun from(member: Member): MemberResponse =
            MemberResponse(
                id = checkNotNull(member.id) { "저장된 회원의 id는 null일 수 없습니다" },
                email = member.email,
                isActive = member.isActive,
                createdAt = member.createdAt,
            )
    }
}

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
) {
    companion object {
        fun from(tokens: TokenPair): TokenResponse =
            TokenResponse(
                accessToken = tokens.accessToken.value,
                refreshToken = tokens.refreshToken.value,
                tokenType = "Bearer",
                expiresIn = tokens.accessToken.ttl.seconds,
            )
    }
}
