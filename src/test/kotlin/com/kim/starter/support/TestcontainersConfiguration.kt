package com.kim.starter.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Testcontainers 기반 통합 테스트 인프라.
 *
 * - `@ServiceConnection`이 PostgreSQL/Redis 컨테이너의 host/port를 Spring Boot의
 *   `DataSource` / `RedisConnectionDetails`에 자동 주입한다 → application-test.yml의
 *   datasource/redis URL 명시 불필요.
 * - PostgreSQL은 ImageName으로부터 inferred. Redis는 GenericContainer라 추론 불가하므로
 *   `name = "redis"` 힌트로 `RedisConnectionDetails`를 생성하도록 한다 (Spring Boot 4 가이드).
 * - 운영 compose.yaml과 동일한 이미지 태그(`postgres:17-alpine`, `redis:7-alpine`)를 사용하여
 *   환경 간 차이를 최소화한다.
 *
 * starter kit 사용자는 [com.kim.starter.support.IntegrationTest] 메타 어노테이션을 통해
 * 자동으로 이 설정을 import한다 — 통합 테스트 클래스가 이 클래스를 직접 참조할 일은 없다.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer =
        PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .withReuse(true)

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> =
        GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(REDIS_PORT)
            .withReuse(true)

    private companion object {
        const val REDIS_PORT = 6379
    }
}
