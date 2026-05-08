package com.kim.starter.api.auth.login

import com.kim.starter.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.assertj.MockMvcTester

@IntegrationTest
@DisplayName("OPTIONS /auth/login (CORS preflight)")
class `OPTIONS_specs`(
    @Autowired private val mvc: MockMvcTester,
) {
    @Test
    fun `허용된 origin의 preflight 요청은 200을 반환한다`() {
        val result =
            mvc
                .options()
                .uri("/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type")
                .exchange()

        assertThat(result).hasStatusOk()
        assertThat(result.response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:3000")
        assertThat(result.response.getHeader("Access-Control-Allow-Methods")).isNotBlank()
    }

    @Test
    fun `허용되지 않은 origin의 요청은 CORS 헤더를 반환하지 않는다`() {
        val result =
            mvc
                .options()
                .uri("/auth/login")
                .header("Origin", "http://evil.example.com")
                .header("Access-Control-Request-Method", "POST")
                .exchange()

        assertThat(result.response.getHeader("Access-Control-Allow-Origin")).isNull()
    }
}
