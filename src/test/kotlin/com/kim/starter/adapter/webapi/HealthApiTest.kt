package com.kim.starter.adapter.webapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * HealthApi 단위 테스트.
 *
 * 의도:
 * - Spring 컨텍스트를 띄우지 않는다 → docker/PostgreSQL/Redis 불필요, 빠름.
 * - Clock DI 패턴 시연: 시간 의존을 인자로 받아 결정론적 검증.
 *
 * 통합 테스트 패턴은 첫 도메인 추가 시 `support/ApplicationApiTest`를 사용해 작성한다.
 * 통합 테스트는 docker desktop이 켜져 있어야 한다 (compose.yaml 자동 기동).
 */
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
