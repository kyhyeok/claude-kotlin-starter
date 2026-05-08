package com.kim.starter.adapter.security

import com.kim.starter.application.required.RefreshTokenStore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

// subject당 1개의 활성 RT만 허용 → rotation 시 이전 RT 자동 무효화 (ADR-0003).
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
