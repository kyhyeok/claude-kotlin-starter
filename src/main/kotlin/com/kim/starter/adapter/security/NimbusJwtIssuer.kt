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

/**
 * Spring Security JwtEncoder + Nimbus 기반 HS256 토큰 발급(ADR-0003).
 *
 * - [subject]는 Member.id 같은 안정적 PK만 사용한다 (CLAUDE.md §6).
 *   변경 가능한 영문 식별자(loginId 등)를 박지 않는다.
 * - Refresh Token은 `typ="refresh"` 클레임으로 Access Token과 구분 →
 *   AT가 RT로 오용되는 흐름을 차단한다.
 */
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
