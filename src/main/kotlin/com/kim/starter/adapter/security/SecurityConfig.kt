package com.kim.starter.adapter.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import javax.crypto.spec.SecretKeySpec

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { headers ->
                // Swagger UI는 인라인 스크립트·스타일을 사용하므로 'unsafe-inline' 허용.
                headers.contentSecurityPolicy {
                    it.policyDirectives(
                        "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; img-src 'self' data:",
                    )
                }
                headers.httpStrictTransportSecurity { it.includeSubDomains(true).maxAgeInSeconds(31536000L) }
                headers.frameOptions { it.deny() }
                headers.referrerPolicy { it.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
                // permissionsPolicy DSL이 Spring Security 7에서 deprecated → HeaderWriter 람다로 직접 박음.
                headers.addHeaderWriter { _, response ->
                    response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
                }
            }
            // CorsConfigurationSource bean을 자동으로 잡음 (CorsConfig.kt).
            .cors { }
            .authorizeHttpRequests {
                it
                    .requestMatchers("/health", "/actuator/health/**")
                    .permitAll()
                    // 운영은 reverse proxy/IP 화이트리스트로 보호 (ADR-0016).
                    .requestMatchers("/actuator/prometheus")
                    .permitAll()
                    .requestMatchers("/swagger-ui.html", "/api-spec/**", "/webjars/**")
                    .permitAll()
                    // 더 구체적인 매처를 먼저 두어야 한다.
                    .requestMatchers(HttpMethod.POST, "/auth/logout")
                    .authenticated()
                    .requestMatchers("/auth/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.oauth2ResourceServer { rs ->
                rs.jwt { }
            }.build()

    @Bean
    fun jwtDecoder(properties: JwtProperties): JwtDecoder {
        val key = SecretKeySpec(properties.secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(key).build()
    }

    @Bean
    fun jwtEncoder(properties: JwtProperties): JwtEncoder {
        val key = SecretKeySpec(properties.secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        return NimbusJwtEncoder(ImmutableSecret<SecurityContext>(key))
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
}
