package com.kim.starter.api.health

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.kim.starter.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.assertj.MockMvcTester

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
