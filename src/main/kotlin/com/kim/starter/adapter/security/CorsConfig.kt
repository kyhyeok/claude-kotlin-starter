package com.kim.starter.adapter.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig(
    private val properties: CorsProperties,
) {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config =
            CorsConfiguration().apply {
                // allowedOrigins가 비면 CORS 비활성 — fork된 서비스가 도메인 확정 후 설정.
                allowedOrigins = properties.allowedOrigins.ifEmpty { null }
                allowedMethods = properties.allowedMethods
                allowedHeaders = properties.allowedHeaders
                exposedHeaders = properties.exposedHeaders
                maxAge = properties.maxAge.seconds
            }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
