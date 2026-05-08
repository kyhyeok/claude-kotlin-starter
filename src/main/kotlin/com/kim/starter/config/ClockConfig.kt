package com.kim.starter.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

// LocalDateTime.now() 직접 호출 금지 (CLAUDE.md §4). 테스트는 Clock.fixed(...)로 override.
@Configuration
class ClockConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
