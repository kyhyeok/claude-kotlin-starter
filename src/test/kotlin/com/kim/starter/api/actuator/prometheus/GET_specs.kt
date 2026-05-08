package com.kim.starter.api.actuator.prometheus

import com.kim.starter.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.assertj.MockMvcTester

@IntegrationTest
@DisplayName("GET /actuator/prometheus")
class `GET_specs`(
    @Autowired private val mvc: MockMvcTester,
) {
    @Test
    fun `200을 반환한다`() {
        val result = mvc.get().uri("/actuator/prometheus").exchange()

        assertThat(result).hasStatusOk()
    }

    @Test
    fun `Prometheus exposition 포맷으로 응답한다`() {
        val result = mvc.get().uri("/actuator/prometheus").exchange()

        assertThat(result)
            .hasStatusOk()
            .bodyText()
            .contains("# HELP")
            .contains("# TYPE")
            .contains("jvm_memory_used_bytes")
    }
}
