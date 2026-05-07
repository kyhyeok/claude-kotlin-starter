package com.kim.starter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * `@ConfigurationPropertiesScan`: `@ConfigurationProperties` 클래스를 자동 등록 →
 * 새 설정 클래스를 추가할 때 별도 `@EnableConfigurationProperties` 선언 불필요.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
class StarterApplication

fun main(args: Array<String>) {
    runApplication<StarterApplication>(*args)
}
