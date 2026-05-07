package com.kim.starter.application.required

import java.time.Duration
import java.time.Instant

/**
 * JWT 발급/검증 포트.
 *
 * 도메인/애플리케이션 계층은 이 인터페이스만 의존한다.
 * 실 구현은 adapter.security.NimbusJwtIssuer (Day 2).
 */
interface JwtIssuer {
    fun issueAccessToken(
        subject: String,
        claims: Map<String, Any> = emptyMap(),
    ): IssuedToken

    fun issueRefreshToken(subject: String): IssuedToken
}

data class IssuedToken(
    val value: String,
    val subject: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
) {
    val ttl: Duration get() = Duration.between(issuedAt, expiresAt)
}
