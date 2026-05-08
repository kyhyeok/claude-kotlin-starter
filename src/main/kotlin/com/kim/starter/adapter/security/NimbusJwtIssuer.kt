package com.kim.starter.adapter.security

import com.kim.starter.application.required.IssuedToken
import com.kim.starter.application.required.JwtIssuer
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Component
class NimbusJwtIssuer(
    private val encoder: JwtEncoder,
    private val clock: Clock,
    private val properties: JwtProperties,
) : JwtIssuer {
    override fun issueAccessToken(
        subject: String,
        claims: Map<String, Any>,
    ): IssuedToken = encode(subject, properties.accessTokenTtl, claims + ("typ" to "access"))

    override fun issueRefreshToken(subject: String): IssuedToken = encode(subject, properties.refreshTokenTtl, mapOf("typ" to "refresh"))

    private fun encode(
        subject: String,
        ttl: Duration,
        claims: Map<String, Any>,
    ): IssuedToken {
        val issuedAt = Instant.now(clock)
        val expiresAt = issuedAt.plus(ttl)
        val claimsSet =
            JwtClaimsSet
                .builder()
                // jti UUID로 같은 초 같은 subject 발급 시 토큰 unique 보장 (RT rotation 결함 fix, ADR-0013).
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claims { it.putAll(claims) }
                .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = encoder.encode(JwtEncoderParameters.from(header, claimsSet))
        return IssuedToken(
            value = token.tokenValue,
            subject = subject,
            issuedAt = issuedAt,
            expiresAt = expiresAt,
        )
    }
}
