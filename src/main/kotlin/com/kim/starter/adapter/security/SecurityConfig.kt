package com.kim.starter.adapter.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.spec.SecretKeySpec

/**
 * JWT 기반 OAuth2 Resource Server 설정.
 *
 * - 알고리즘: HS256 (대칭키)
 * - 시크릿: 환경변수 JWT_SECRET (application.yml에서 주입)
 * - 토큰 검증: NimbusJwtDecoder (Spring Security 표준)
 *
 * Refresh Token은 별도로 관리 (RefreshTokenStore 포트, RedisRefreshTokenStore 어댑터).
 *
 * 자세한 설계 의도: ADR-0003 참고.
 */
@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers("/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/auth/**")
                    .permitAll() // 로그인/토큰 발급은 토큰 없이 호출
                    .anyRequest()
                    .authenticated()
            }.oauth2ResourceServer { rs ->
                rs.jwt { /* default JwtAuthenticationConverter */ }
            }.build()

    @Bean
    fun jwtDecoder(
        @Value("\${security.jwt.secret}") secret: String,
    ): JwtDecoder {
        val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(key).build()
    }

    @Bean
    fun jwtEncoder(
        @Value("\${security.jwt.secret}") secret: String,
    ): JwtEncoder {
        val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        return NimbusJwtEncoder(ImmutableSecret<SecurityContext>(key))
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
}
