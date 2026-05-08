package com.kim.starter.support

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester

// 매 테스트가 독립 회원·토큰을 만들도록 한다 (CLAUDE.md §5).
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
