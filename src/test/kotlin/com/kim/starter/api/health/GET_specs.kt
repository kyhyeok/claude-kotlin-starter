package com.kim.starter.api.health

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.kim.starter.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.assertj.MockMvcTester

/**
 * `GET /health` 통합 테스트.
 *
 * - 익명 호출 허용([com.kim.starter.adapter.security.SecurityConfig]에서 permitAll).
 * - timestamp는 [java.time.Clock] 빈을 통해 결정되며 통합 테스트는 system clock 사용 → 값 자체는
 *   non-blank만 검증한다(시점 의존 자체가 통합 테스트의 검증 포인트가 아니므로).
 */
@IntegrationTest
@DisplayName("GET /health")
class `GET_specs`(
    @Autowired private val mvc: MockMvcTester,
) {
    @Test
    fun `200을 반환한다`() {
        val result = mvc.get().uri("/health").exchange()

        assertThat(result).hasStatusOk()
    }

    @Test
    fun `status UP과 timestamp를 반환한다`() {
        val result = mvc.get().uri("/health").exchange()

        assertThat(result)
            .apply(document("health-get"))
            .hasStatusOk()
            .bodyJson()
            .hasPathSatisfying("$.status") { it.assertThat().asString().isEqualTo("UP") }
            .hasPathSatisfying("$.timestamp") { it.assertThat().asString().isNotBlank() }
    }
}
