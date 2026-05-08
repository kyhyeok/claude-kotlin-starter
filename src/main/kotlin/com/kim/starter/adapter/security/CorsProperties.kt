package com.kim.starter.adapter.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = emptyList(),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
    val allowedHeaders: List<String> = listOf("*"),
    val exposedHeaders: List<String> = listOf("Authorization"),
    val maxAge: Duration = Duration.ofHours(1),
)
