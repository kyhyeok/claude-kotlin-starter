package com.kim.starter.adapter.webapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

// Spring 컨텍스트 없이 Clock DI 패턴 시연. 통합 테스트는 api/health/GET_specs.kt.
@DisplayName("HealthApi")
class HealthApiTest {
    private val fixedClock = Clock.fixed(Instant.parse("2026-05-06T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `health는 status UP을 반환한다`() {
        val api = HealthApi(fixedClock)

        val response = api.health()

        assertThat(response.status).isEqualTo("UP")
    }

    @Test
    fun `health는 주입받은 Clock 기반의 timestamp를 반환한다`() {
        val api = HealthApi(fixedClock)

        val response = api.health()

        assertThat(response.timestamp).isEqualTo(Instant.parse("2026-05-06T00:00:00Z"))
    }
}
