package com.kim.starter.adapter.security.ratelimit

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration

class RateLimitInterceptor(
    private val redisTemplate: StringRedisTemplate,
    private val properties: RateLimitProperties,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val path = request.requestURI
        val rule = properties.rules.find { it.path == path } ?: return true

        val ip =
            request
                .getHeader("X-Forwarded-For")
                ?.split(",")
                ?.first()
                ?.trim()
                ?: request.remoteAddr
        // 분 단위 고정 윈도우 — 키가 다른 분에 만들어지면 자동으로 새 카운터 시작.
        val window = System.currentTimeMillis() / 60_000
        val key = "ratelimit:$path:$ip:$window"

        val ops = redisTemplate.opsForValue()
        val count = ops.increment(key) ?: 1L
        if (count == 1L) redisTemplate.expire(key, Duration.ofSeconds(60))

        if (count > rule.limit) {
            response.status = 429
            response.setHeader("Retry-After", "60")
            return false
        }
        return true
    }
}
