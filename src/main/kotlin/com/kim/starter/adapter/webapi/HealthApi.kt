package com.kim.starter.adapter.webapi

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant

/**
 * 부팅 검증용 단일 엔드포인트.
 * starter kit을 fork한 첫 프로젝트는 이 컨트롤러를 지우거나 자체 엔드포인트로 교체하면 된다.
 */
@RestController
class HealthApi(
    private val clock: Clock,
) {
    @GetMapping("/health")
    fun health(): HealthResponse = HealthResponse(status = "UP", timestamp = Instant.now(clock))

    data class HealthResponse(
        val status: String,
        val timestamp: Instant,
    )
}
