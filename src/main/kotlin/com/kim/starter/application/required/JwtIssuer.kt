package com.kim.starter.application.required

import java.time.Duration
import java.time.Instant

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
