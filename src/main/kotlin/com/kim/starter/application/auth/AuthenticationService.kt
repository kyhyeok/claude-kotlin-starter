package com.kim.starter.application.auth

import com.kim.starter.application.auth.provided.LoginAuthenticator
import com.kim.starter.application.auth.provided.LogoutHandler
import com.kim.starter.application.auth.provided.TokenRefresher
import com.kim.starter.application.required.JwtIssuer
import com.kim.starter.application.required.MemberRepository
import com.kim.starter.application.required.RefreshTokenStore
import com.kim.starter.domain.member.InvalidCredentialException
import com.kim.starter.domain.member.Member
import com.kim.starter.domain.member.MemberNotActiveException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 인증 흐름 단일 컨텍스트의 3개 Use Case(login/refresh/logout) 구현.
 *
 * 정책:
 * - JWT subject는 [Member.id]의 String 표현 (안정적 PK, CLAUDE.md §6).
 * - login 실패 사유는 모두 [InvalidCredentialException]로 단일화 → 이메일 존재 leak 방지.
 *   단, 활성 상태가 아닌 회원의 로그인은 [MemberNotActiveException]으로 명시 분리한다 →
 *   클라이언트가 "비활성 회원" 안내를 별도로 표시할 수 있도록.
 * - refresh는 `typ=refresh` 클레임 검증 + Redis 활성 RT 일치 검증 → AT 도용/RT 재사용 차단.
 * - rotation: refresh 성공 시 RT 신규 발급 + Redis 교체.
 */
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
        if (!member.isActive) {
            throw MemberNotActiveException(member.currentStatus)
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
