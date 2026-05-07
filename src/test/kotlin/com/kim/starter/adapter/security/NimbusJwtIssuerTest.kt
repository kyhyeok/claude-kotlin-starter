package com.kim.starter.adapter.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import javax.crypto.spec.SecretKeySpec

/**
 * NimbusJwtIssuer 단위 테스트.
 *
 * Spring 컨텍스트를 띄우지 않는다. 토큰을 발급한 뒤 JwtDecoder로 직접 파싱하여
 * subject / exp / typ 클레임을 검증한다.
 */
@DisplayName("NimbusJwtIssuer")
class NimbusJwtIssuerTest {
    private val secret = "test-secret-must-be-at-least-32-bytes-long-for-hs256"
    private val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
    private val encoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(key))

    /**
     * fixed clock 기반 발급 토큰을 시스템 실시간 시계로 검증하면
     * `JwtTimestampValidator`가 만료/iat 미스매치로 거부한다.
     * 단위 테스트는 서명/클레임 파싱만 검증하므로 timestamp validator를 비활성화한다.
     */
    private val decoder =
        NimbusJwtDecoder
            .withSecretKey(key)
            .build()
            .apply {
                setJwtValidator(OAuth2TokenValidator<Jwt> { OAuth2TokenValidatorResult.success() })
            }
    private val fixedNow = Instant.parse("2026-05-07T00:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
    private val properties =
        JwtProperties(
            secret = secret,
            accessTokenTtl = Duration.ofMinutes(15),
            refreshTokenTtl = Duration.ofDays(7),
        )
    private val issuer = NimbusJwtIssuer(encoder, clock, properties)

    @Test
    fun `Access Token은 subject와 exp를 클레임에 박는다`() {
        val token = issuer.issueAccessToken(subject = "42")

        val parsed = decoder.decode(token.value)

        assertThat(parsed.subject).isEqualTo("42")
        assertThat(parsed.issuedAt).isEqualTo(fixedNow)
        assertThat(parsed.expiresAt).isEqualTo(fixedNow.plus(Duration.ofMinutes(15)))
        assertThat(parsed.getClaim<String>("typ")).isEqualTo("access")
    }

    @Test
    fun `Access Token은 추가 클레임을 보존한다`() {
        val token = issuer.issueAccessToken(subject = "42", claims = mapOf("role" to "USER"))

        val parsed = decoder.decode(token.value)

        assertThat(parsed.getClaim<String>("role")).isEqualTo("USER")
    }

    @Test
    fun `Refresh Token은 typ refresh 클레임으로 Access Token과 구분한다`() {
        val token = issuer.issueRefreshToken(subject = "42")

        val parsed = decoder.decode(token.value)

        assertThat(parsed.subject).isEqualTo("42")
        assertThat(parsed.getClaim<String>("typ")).isEqualTo("refresh")
        assertThat(parsed.expiresAt).isEqualTo(fixedNow.plus(Duration.ofDays(7)))
    }

    @Test
    fun `IssuedToken의 ttl은 issuedAt과 expiresAt의 차이로 계산한다`() {
        val token = issuer.issueAccessToken(subject = "42")

        assertThat(token.ttl).isEqualTo(Duration.ofMinutes(15))
        assertThat(token.subject).isEqualTo("42")
        assertThat(token.issuedAt).isEqualTo(fixedNow)
        assertThat(token.expiresAt).isEqualTo(fixedNow.plus(Duration.ofMinutes(15)))
    }

    @Test
    fun `매 발급마다 jti 클레임에 unique UUID를 박는다`() {
        val first = issuer.issueRefreshToken(subject = "42")
        val second = issuer.issueRefreshToken(subject = "42")

        val firstId = decoder.decode(first.value).id
        val secondId = decoder.decode(second.value).id

        assertThat(firstId).isNotBlank()
        assertThat(secondId).isNotBlank().isNotEqualTo(firstId)
        assertThat(first.value).isNotEqualTo(second.value) // jti 덕분에 같은 초에도 토큰이 다름
    }
}
