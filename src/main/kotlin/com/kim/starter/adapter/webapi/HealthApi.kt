package com.kim.starter.adapter.webapi

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant

// 부팅 검증용. fork한 프로젝트는 자체 엔드포인트로 교체하거나 제거.
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
