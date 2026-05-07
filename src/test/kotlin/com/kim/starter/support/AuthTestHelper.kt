package com.kim.starter.support

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester

/**
 * Auth 통합 테스트의 공통 흐름(등록 → 로그인 → 토큰 추출).
 *
 * 각 spec이 독립적인 회원·토큰을 생성하도록 하여 테스트 간 격리를 보장한다(CLAUDE.md §5).
 * - 하드코딩 PK·재사용 fixture를 두지 않음.
 * - logout/refresh spec은 [issueTokens]로 즉석 회원 + 토큰 쌍을 받아 사용.
 */
object AuthTestHelper {
    private val mapper = ObjectMapper()

    fun register(
        mvc: MockMvcTester,
        email: String,
        password: String,
    ) {
        mvc
            .post()
            .uri("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email": "$email", "password": "$password"}""")
            .exchange()
    }

    fun issueTokens(
        mvc: MockMvcTester,
        email: String,
        password: String,
    ): TokenResponseFixture {
        register(mvc, email, password)
        val response =
            mvc
                .post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "$email", "password": "$password"}""")
                .exchange()
        val body: JsonNode = mapper.readTree(response.response.contentAsString)
        return TokenResponseFixture(
            accessToken = body.get("accessToken").asText(),
            refreshToken = body.get("refreshToken").asText(),
        )
    }

    data class TokenResponseFixture(
        val accessToken: String,
        val refreshToken: String,
    )
}
