package com.kim.starter.api.auth.register

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
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
 * `POST /auth/register` 통합 테스트.
 *
 * - 신규 회원 등록 → 201 Created + MemberResponse.
 * - 동일 이메일 중복 등록 → 409 Conflict ([com.kim.starter.domain.member.DuplicateEmailException] 매핑).
 * - 잘못된 입력(이메일 형식/짧은 비밀번호) → 400 Bad Request (Bean Validation).
 */
@IntegrationTest
@DisplayName("POST /auth/register")
class `POST_specs`(
    @Autowired private val mvc: MockMvcTester,
) {
    @Test
    fun `201로 회원을 등록한다`() {
        val email = EmailGenerator.generateEmail()
        val password = PasswordGenerator.generatePassword()

        val result =
            mvc
                .post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "$email", "password": "$password"}""")
                .exchange()

        assertThat(result)
            .apply(document("auth-register"))
            .hasStatus(201)
            .bodyJson()
            .hasPathSatisfying("$.id") { it.assertThat().asNumber().isNotNull() }
            .hasPathSatisfying("$.email") { it.assertThat().asString().isEqualTo(email) }
            .hasPathSatisfying("$.isActive") { it.assertThat().asBoolean().isTrue() }
    }

    @Test
    fun `중복 이메일은 409를 반환한다`() {
        val email = EmailGenerator.generateEmail()
        val password = PasswordGenerator.generatePassword()
        val body = """{"email": "$email", "password": "$password"}"""

        mvc
            .post()
            .uri("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .exchange()
        val result =
            mvc
                .post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange()

        assertThat(result).hasStatus(409)
    }

    @Test
    fun `잘못된 이메일 형식은 400을 반환한다`() {
        val result =
            mvc
                .post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "not-an-email", "password": "${PasswordGenerator.generatePassword()}"}""")
                .exchange()

        assertThat(result).hasStatus(400)
    }

    @Test
    fun `짧은 비밀번호는 400을 반환한다`() {
        val result =
            mvc
                .post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "${EmailGenerator.generateEmail()}", "password": "short"}""")
                .exchange()

        assertThat(result).hasStatus(400)
    }
}
