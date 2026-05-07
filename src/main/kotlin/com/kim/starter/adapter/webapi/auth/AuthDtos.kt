package com.kim.starter.adapter.webapi.auth

import com.kim.starter.application.auth.TokenPair
import com.kim.starter.domain.member.Member
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

/**
 * 인증 API 요청/응답 DTO.
 *
 * - 도메인 [com.kim.starter.domain.member.Email] VO와 이름이 충돌하므로 jakarta validation의
 *   `@Email`을 fqcn(`@jakarta.validation.constraints.Email`)으로 명시한다.
 * - 응답은 OAuth 2.0 토큰 응답 컨벤션을 따른다(tokenType / expiresIn-seconds).
 */
data class RegisterRequest(
    @field:NotBlank
    @field:jakarta.validation.constraints.Email
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    val password: String,
)

data class LoginRequest(
    @field:NotBlank
    @field:jakarta.validation.constraints.Email
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
    val status: String,
    val createdAt: OffsetDateTime,
) {
    companion object {
        fun from(member: Member): MemberResponse =
            MemberResponse(
                id = checkNotNull(member.id) { "저장된 회원의 id는 null일 수 없습니다" },
                email = member.email.value,
                status = member.currentStatus.name,
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
