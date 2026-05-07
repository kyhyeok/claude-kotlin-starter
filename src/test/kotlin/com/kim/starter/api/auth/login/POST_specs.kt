package com.kim.starter.api.auth.login

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.kim.starter.support.AuthTestHelper.register
import com.kim.starter.support.IntegrationTest
import com.kim.starter.support.generator.EmailGenerator
import com.kim.starter.support.generator.PasswordGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester

/**
 * `POST /auth/login` 통합 테스트.
 *
 * - 등록된 회원의 정상 로그인 → 200 + accessToken/refreshToken 반환.
 * - 미등록 이메일 → 401 (이메일 존재 leak 방지를 위해 InvalidCredential 단일화).
 * - 비밀번호 불일치 → 401.
 */
@IntegrationTest
@DisplayName("POST /auth/login")
class `POST_specs`(
    @Autowired private val mvc: MockMvcTester,
) {
    @Test
    fun `200으로 토큰을 발급한다`() {
        val email = EmailGenerator.generateEmail()
        val password = PasswordGenerator.generatePassword()
        register(mvc, email, password)

        val result =
            mvc
                .post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "$email", "password": "$password"}""")
                .exchange()

        assertThat(result)
            .apply(document("auth-login"))
            .hasStatusOk()
            .bodyJson()
            .hasPathSatisfying("$.accessToken") { it.assertThat().asString().isNotBlank() }
            .hasPathSatisfying("$.refreshToken") { it.assertThat().asString().isNotBlank() }
            .hasPathSatisfying("$.tokenType") { it.assertThat().asString().isEqualTo("Bearer") }
            .hasPathSatisfying("$.expiresIn") { it.assertThat().asNumber().isEqualTo(900) }
    }

    @Test
    fun `미등록 이메일은 401을 반환한다`() {
        val result =
            mvc
                .post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"email": "${EmailGenerator.generateEmail()}", "password": "${PasswordGenerator.generatePassword()}"}""",
                ).exchange()

        assertThat(result).hasStatus(401)
    }

    @Test
    fun `비밀번호 불일치는 401을 반환한다`() {
        val email = EmailGenerator.generateEmail()
        register(mvc, email, PasswordGenerator.generatePassword())

        val result =
            mvc
                .post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "$email", "password": "${PasswordGenerator.generatePassword()}"}""")
                .exchange()

        assertThat(result).hasStatus(401)
    }
}
