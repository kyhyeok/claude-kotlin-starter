package com.kim.starter.adapter.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * `security.jwt.*` 설정 바인딩.
 *
 * - [secret]: HS256 대칭키. 운영에서는 환경변수 `JWT_SECRET`로 주입(ADR-0004).
 * - [accessTokenTtl]: Access Token 만료 (기본 PT15M, ADR-0003).
 * - [refreshTokenTtl]: Refresh Token 만료 (기본 P7D, ADR-0003).
 *
 * @ConfigurationPropertiesScan 으로 자동 등록한다 (StarterApplication 참고).
 */
@ConfigurationProperties(prefix = "security.jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenTtl: Duration,
    val refreshTokenTtl: Duration,
)
