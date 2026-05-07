package com.kim.starter.application.required

import java.time.Duration

/**
 * Refresh Token 저장소 포트.
 *
 * 실 구현은 adapter.persistence.RedisRefreshTokenStore (Day 2).
 * 테스트에서는 InMemoryRefreshTokenStore를 사용한다.
 */
interface RefreshTokenStore {
    fun save(
        subject: String,
        token: String,
        ttl: Duration,
    )

    fun find(subject: String): String?

    fun revoke(subject: String)
}
