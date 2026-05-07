package com.kim.starter.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles

/**
 * 모든 API 통합 테스트가 사용하는 메타 어노테이션.
 *
 * 사용:
 * ```
 * @ApplicationApiTest
 * @DisplayName("GET /health")
 * class `GET_specs` { ... }
 * ```
 *
 * 새로운 테스트 설정이 필요하면 여기 한 군데만 수정.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
annotation class ApplicationApiTest
