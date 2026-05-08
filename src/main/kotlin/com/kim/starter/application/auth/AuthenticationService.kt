package com.kim.starter.application.auth

import com.kim.starter.application.auth.provided.LoginAuthenticator
import com.kim.starter.application.auth.provided.LogoutHandler
import com.kim.starter.application.auth.provided.TokenRefresher
import com.kim.starter.application.required.JwtIssuer
import com.kim.starter.application.required.MemberRepository
import com.kim.starter.application.required.RefreshTokenStore
import com.kim.starter.domain.member.InvalidCredentialException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 정책 (ADR-0003, ADR-0012, ADR-0018):
// - JWT subject = Member.id (안정적 PK).
// - login 실패는 InvalidCredential 단일화 (이메일 leak 방지).
// - refresh: typ=refresh + Redis 활성 RT 일치 → AT 도용/RT 재사용 차단 + rotation.
// - 비활성 회원 차단은 starter scope 외 — fork에서 isActive 검사 추가.
@Service
@Transactional
class AuthenticationService(
    private val members: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtIssuer: JwtIssuer,
    private val jwtDecoder: JwtDecoder,
    private val refreshTokenStore: RefreshTokenStore,
) : LoginAuthenticator,
    TokenRefresher,
    LogoutHandler {
    override fun login(command: LoginAuthenticator.LoginCommand): TokenPair {
        val member = members.findByEmail(command.email) ?: throw InvalidCredentialException()
        if (!passwordEncoder.matches(command.rawPassword, member.passwordHash)) {
            throw InvalidCredentialException()
        }
        val subject = checkNotNull(member.id) { "저장된 회원의 id는 null일 수 없습니다" }.toString()
        return issueAndPersist(subject)
    }

    override fun refresh(refreshToken: String): TokenPair {
        val parsed =
            try {
                jwtDecoder.decode(refreshToken)
            } catch (_: JwtException) {
                throw InvalidCredentialException()
            }
        if (parsed.getClaim<String?>("typ") != "refresh") {
            throw InvalidCredentialException()
        }
        val subject = parsed.subject ?: throw InvalidCredentialException()
        val storedRt = refreshTokenStore.find(subject)
        if (storedRt == null || storedRt != refreshToken) {
            throw InvalidCredentialException()
        }
        return issueAndPersist(subject)
    }

    override fun logout(memberId: Long) {
        refreshTokenStore.revoke(memberId.toString())
    }

    private fun issueAndPersist(subject: String): TokenPair {
        val accessToken = jwtIssuer.issueAccessToken(subject)
        val refreshToken = jwtIssuer.issueRefreshToken(subject)
        refreshTokenStore.save(subject, refreshToken.value, refreshToken.ttl)
        return TokenPair(accessToken, refreshToken)
    }
}
