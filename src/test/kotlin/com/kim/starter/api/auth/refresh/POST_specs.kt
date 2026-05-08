package com.kim.starter.api.auth.refresh

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.kim.starter.support.AuthTestHelper
import com.kim.starter.support.IntegrationTest
import com.kim.starter.support.generator.EmailGenerator
import com.kim.starter.support.generator.PasswordGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester

@IntegrationTest
@DisplayName("POST /auth/refresh")
class `POST_specs`(
    @Autowired private val mvc: MockMvcTester,
) {
    @Test
    fun `200으로 새 토큰을 발급한다`() {
        val tokens = AuthTestHelper.issueTokens(mvc, EmailGenerator.generateEmail(), PasswordGenerator.generatePassword())

        val result =
            mvc
                .post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "${tokens.refreshToken}"}""")
                .exchange()

        assertThat(result)
            .apply(document("auth-refresh"))
            .hasStatusOk()
            .bodyJson()
            .hasPathSatisfying("$.accessToken") { it.assertThat().asString().isNotBlank() }
            .hasPathSatisfying("$.refreshToken") {
                it
                    .assertThat()
                    .asString()
                    .isNotBlank()
                    .isNotEqualTo(tokens.refreshToken)
            }
    }

    @Test
    fun `잘못된 토큰은 401을 반환한다`() {
        val result =
            mvc
                .post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "not-a-valid-jwt"}""")
                .exchange()

        assertThat(result).hasStatus(401)
    }

    @Test
    fun `AT를 RT 자리에 넣으면 401을 반환한다`() {
        val tokens = AuthTestHelper.issueTokens(mvc, EmailGenerator.generateEmail(), PasswordGenerator.generatePassword())

        val result =
            mvc
                .post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "${tokens.accessToken}"}""")
                .exchange()

        assertThat(result).hasStatus(401)
    }

    @Test
    fun `이미 rotation된 RT는 401을 반환한다`() {
        val tokens = AuthTestHelper.issueTokens(mvc, EmailGenerator.generateEmail(), PasswordGenerator.generatePassword())
        mvc
            .post()
            .uri("/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"refreshToken": "${tokens.refreshToken}"}""")
            .exchange()

        val result =
            mvc
                .post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "${tokens.refreshToken}"}""")
                .exchange()

        assertThat(result).hasStatus(401)
    }
}
