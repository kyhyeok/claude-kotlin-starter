package com.kim.starter.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * 시간 의존성을 Bean으로 노출.
 *
 * 도메인/애플리케이션 코드는 [Clock]을 주입받아 사용한다.
 * 테스트에서는 Clock.fixed(...) 를 [Bean]으로 override하여 시간을 통제한다.
 *
 * 절대 LocalDateTime.now() 직접 호출을 코드에 박지 말 것.
 */
@Configuration
class ClockConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
