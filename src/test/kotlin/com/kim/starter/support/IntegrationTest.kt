package com.kim.starter.support

import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * 모든 통합 테스트가 사용하는 메타 어노테이션.
 *
 * 포함하는 설정:
 * - `@SpringBootTest`: 전체 컨텍스트 기동 + MOCK 환경 (RANDOM_PORT 불필요 — MockMvc로 충분).
 * - `@AutoConfigureMockMvc`: `MockMvcTester` 빈을 자동 등록 (Spring Boot 4의 fluent API).
 * - `@AutoConfigureRestDocs`: REST Docs 스니펫을 `build/generated-snippets/`에 생성.
 *   `outputDir`은 default(`build/generated-snippets`)를 그대로 사용 →
 *   `restdocs-api-spec` plugin이 같은 위치에서 OpenAPI 3 spec을 합성한다.
 * - `@ActiveProfiles("test")`: `application-test.yml`을 추가 로드.
 * - `@Import(TestcontainersConfiguration)`: PostgreSQL/Redis Testcontainers를
 *   `@ServiceConnection`으로 자동 주입.
 *
 * 사용:
 * ```
 * @IntegrationTest
 * @DisplayName("GET /health")
 * class `GET_specs`(
 *     @Autowired private val mvc: MockMvcTester,
 * ) { ... }
 * ```
 *
 * 새로운 테스트 인프라(예: WireMock, Kafka Testcontainers)가 필요하면 여기 한 곳만 수정한다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
annotation class IntegrationTest
