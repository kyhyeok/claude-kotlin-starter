package com.kim.starter.support.assertion

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import java.util.Base64

// decoder 없이 토큰의 형식만 검증 (3-segment + base64url-encoded JSON). subject/claim 검증은 호출처에서 decoder 사용.
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
