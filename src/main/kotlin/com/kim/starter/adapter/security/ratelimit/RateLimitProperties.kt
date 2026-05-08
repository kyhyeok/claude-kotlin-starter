package com.kim.starter.adapter.security.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.ratelimit")
data class RateLimitProperties(
    val rules: List<RuleConfig> = emptyList(),
) {
    data class RuleConfig(
        val path: String,
        val limit: Long,
    )
}
