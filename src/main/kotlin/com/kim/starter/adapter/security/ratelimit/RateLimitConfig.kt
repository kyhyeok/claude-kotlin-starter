package com.kim.starter.adapter.security.ratelimit

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class RateLimitConfig(
    private val redisTemplate: StringRedisTemplate,
    private val properties: RateLimitProperties,
) : WebMvcConfigurer {
    @Bean
    fun rateLimitInterceptor(): RateLimitInterceptor = RateLimitInterceptor(redisTemplate, properties)

    override fun addInterceptors(registry: InterceptorRegistry) {
        if (properties.rules.isEmpty()) return
        registry
            .addInterceptor(rateLimitInterceptor())
            .addPathPatterns(properties.rules.map { it.path })
    }
}
