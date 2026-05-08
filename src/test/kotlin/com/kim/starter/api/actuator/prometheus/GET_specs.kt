package com.kim.starter.api.actuator.prometheus

import com.kim.starter.application.member.provided.MemberRegister
import com.kim.starter.domain.member.Email
import com.kim.starter.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.assertj.MockMvcTester

/**
 * `GET /actuator/prometheus` 통합 테스트.
 *
 * - 익명 호출 허용([com.kim.starter.adapter.security.SecurityConfig]에서 permitAll, ADR-0016).
 * - 응답 본문은 JVM/Hibernate/Hikari/HTTP 자동 메트릭이 포함되어 수십 KB. 검증은 Prometheus
 *   exposition 포맷의 핵심 시그니처(`# HELP`, `# TYPE`)와 알려진 표준 메트릭(`jvm_memory_used_bytes`)
 *   substring 매칭으로 충분하다.
 * - Content-Type은 Accept 헤더에 따라 `text/plain; version=0.0.4` 또는
 *   `application/openmetrics-text; version=1.0.0`. 둘 다 수용한다.
 */
@IntegrationTest
@DisplayName("GET /actuator/prometheus")
class `GET_specs`(
    @Autowired private val mvc: MockMvcTester,
    @Autowired private val memberRegister: MemberRegister,
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

    @Test
    fun `member_registration 도메인 카운터를 노출한다`() {
        // 매 테스트가 독립 데이터를 만들도록 unique email 사용 (CLAUDE.md §5).
        memberRegister.register(
            MemberRegister.RegisterCommand(
                email = Email("metrics-${System.nanoTime()}@example.com"),
                rawPassword = "P@ssw0rd!",
            ),
        )

        val result = mvc.get().uri("/actuator/prometheus").exchange()

        // Micrometer Prometheus는 카운터에 `_total` 접미사를 붙이고 dot을 underscore로 치환한다
        // → `member.registration` → `member_registration_total`.
        assertThat(result)
            .hasStatusOk()
            .bodyText()
            .contains("member_registration_total")
            .contains("""result="success"""")
    }
}
