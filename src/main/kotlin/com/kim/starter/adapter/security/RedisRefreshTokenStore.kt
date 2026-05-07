package com.kim.starter.adapter.security

import com.kim.starter.application.required.RefreshTokenStore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Refresh Token 활성 토큰 저장소(ADR-0003).
 *
 * - 키 스키마: `auth:refresh:{subject}` → 활성 RT 값.
 * - subject당 1개의 활성 RT만 허용 → rotation 시 이전 RT가 자동 무효화된다.
 * - TTL은 RT의 만료시각과 동기화 → 만료된 키는 Redis가 자동 정리(별도 cron 불필요).
 *
 * 멀티 디바이스 동시 로그인이 필요해지면 키에 deviceId/jti를 분기하여 확장한다(Day 3+ 후보).
 */
@Component
class RedisRefreshTokenStore(
    private val redis: StringRedisTemplate,
) : RefreshTokenStore {
    override fun save(
        subject: String,
        token: String,
        ttl: Duration,
    ) {
        redis.opsForValue().set(key(subject), token, ttl)
    }

    override fun find(subject: String): String? = redis.opsForValue().get(key(subject))

    override fun revoke(subject: String) {
        redis.delete(key(subject))
    }

    private fun key(subject: String): String = "$KEY_PREFIX$subject"

    companion object {
        private const val KEY_PREFIX = "auth:refresh:"
    }
}
