package com.kim.starter.adapter.security.ratelimit

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations

@DisplayName("RateLimitInterceptor")
class RateLimitInterceptorTest {
    private val redisTemplate = mockk<StringRedisTemplate>()
    private val ops = mockk<ValueOperations<String, String>>()
    private val properties =
        RateLimitProperties(
            rules = listOf(RateLimitProperties.RuleConfig(path = "/auth/login", limit = 3)),
        )
    private val interceptor = RateLimitInterceptor(redisTemplate, properties)

    private val request = mockk<HttpServletRequest>()
    private val response = mockk<HttpServletResponse>(relaxed = true)

    @BeforeEach
    fun setup() {
        every { redisTemplate.opsForValue() } returns ops
        every { redisTemplate.expire(any(), any()) } returns true
        every { request.requestURI } returns "/auth/login"
        every { request.getHeader("X-Forwarded-For") } returns null
        every { request.remoteAddr } returns "127.0.0.1"
    }

    @Test
    fun `한도 이내 요청은 통과한다`() {
        every { ops.increment(any()) } returns 1L

        val result = interceptor.preHandle(request, response, Any())

        assertThat(result).isTrue()
    }

    @Test
    fun `한도 초과 요청은 429와 Retry-After를 반환하고 차단한다`() {
        every { ops.increment(any()) } returns 4L

        val result = interceptor.preHandle(request, response, Any())

        assertThat(result).isFalse()
        verify { response.status = 429 }
        verify { response.setHeader("Retry-After", "60") }
    }

    @Test
    fun `규칙 없는 경로는 통과한다`() {
        every { request.requestURI } returns "/health"

        val result = interceptor.preHandle(request, response, Any())

        assertThat(result).isTrue()
    }

    @Test
    fun `X-Forwarded-For 헤더가 있으면 그 값을 IP로 사용한다`() {
        every { request.getHeader("X-Forwarded-For") } returns "203.0.113.1, 10.0.0.1"
        every { ops.increment(match { it.contains("203.0.113.1") }) } returns 1L

        interceptor.preHandle(request, response, Any())

        verify { ops.increment(match { it.contains("203.0.113.1") }) }
    }
}
