package com.kim.starter.support.assertion

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import java.util.Base64

/**
 * JWT 단언 (commerce-main `JwtAssertions` 정수 추출).
 *
 * decoder 없이도 토큰 문자열의 **형식 자체**(3-segment + base64url-encoded JSON)를 검증한다.
 * subject/claim 같은 디코딩 의존 단언은 컨텍스트마다 decoder가 달라 호출처에서
 * `NimbusJwtDecoder`를 직접 사용한다(NimbusJwtIssuerTest 참고).
 *
 * 사용:
 * ```
 * assertThat(token.value).satisfies(JwtAssertions.conformsToJwtFormat())
 * ```
 */
object JwtAssertions {
    private val mapper = ObjectMapper()

    fun conformsToJwtFormat(): ThrowingConsumer<String> =
        ThrowingConsumer { token ->
            val parts = token.split(".")
            assertThat(parts).hasSize(3)
            assertThat(parts[0]).matches(::isBase64UrlEncodedJson)
            assertThat(parts[1]).matches(::isBase64UrlEncodedJson)
            assertThat(parts[2]).matches(::isBase64UrlEncoded)
        }

    private fun isBase64UrlEncodedJson(s: String): Boolean =
        try {
            mapper.readTree(Base64.getUrlDecoder().decode(s))
            true
        } catch (_: Exception) {
            false
        }

    private fun isBase64UrlEncoded(s: String): Boolean =
        try {
            Base64.getUrlDecoder().decode(s)
            true
        } catch (_: Exception) {
            false
        }
}
