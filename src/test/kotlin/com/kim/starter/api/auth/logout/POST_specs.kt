package com.kim.starter.api.auth.logout

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.kim.starter.support.AuthTestHelper
import com.kim.starter.support.IntegrationTest
import com.kim.starter.support.generator.EmailGenerator
import com.kim.starter.support.generator.PasswordGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester

/**
 * `POST /auth/logout` 통합 테스트.
 *
 * - logout만 인증 필요(SecurityConfig). subject = 본인 Access Token의 sub 클레임.
 * - 로그아웃 후 동일 RT로 refresh 시도 → 401 (Redis에서 RT 폐기됨을 검증).
 * - AT 없이 호출 → 401.
 */
@IntegrationTest
@DisplayName("POST /auth/logout")
class `POST_specs`(
    @Autowired private val mvc: MockMvcTester,
) {
    @Test
    fun `204로 로그아웃한다`() {
        val tokens = AuthTestHelper.issueTokens(mvc, EmailGenerator.generateEmail(), PasswordGenerator.generatePassword())

        val result =
            mvc
                .post()
                .uri("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokens.accessToken}")
                .exchange()

        assertThat(result).apply(document("auth-logout")).hasStatus(204)
    }

    @Test
    fun `로그아웃 후 동일 RT의 refresh는 401을 반환한다`() {
        val tokens = AuthTestHelper.issueTokens(mvc, EmailGenerator.generateEmail(), PasswordGenerator.generatePassword())

        mvc
            .post()
            .uri("/auth/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokens.accessToken}")
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

    @Test
    fun `AT 없이 호출하면 401을 반환한다`() {
        val result = mvc.post().uri("/auth/logout").exchange()

        assertThat(result).hasStatus(401)
    }
}
