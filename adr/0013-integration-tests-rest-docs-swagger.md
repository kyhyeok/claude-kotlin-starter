# 0013. 통합 테스트 + REST Docs + Swagger UI

- 일자: 2026-05-07
- 상태: Accepted

## 배경

starter kit은 면접/실무에서 헷갈렸던 두 책임을 분리해 박는다.

- **테스트는 행위 단위로 검증** (CLAUDE.md §5) — `MockMvcTester` + Testcontainers + `_specs.kt` 컨벤션.
- **문서는 코드의 결과물** — REST Docs로 만든 OpenAPI spec만 노출. Swagger UI는 그 spec을 읽어 화면에 보여줄 뿐.

이 두 가지가 같은 빌드 사이클에 묶여야 "통과한 테스트만 문서에 남는다" 원칙을 강제할 수 있다.

## 결정

### 1. 통합 테스트 인프라

- `@IntegrationTest` 메타 어노테이션이 `@SpringBootTest + @AutoConfigureMockMvc + @AutoConfigureRestDocs + @ActiveProfiles("test") + @Import(TestcontainersConfiguration)`을 묶는다. 새 인프라 추가는 한 곳만 수정.
- `TestcontainersConfiguration`은 `@ServiceConnection` 기반 PostgreSQL/Redis 컨테이너를 빈으로 등록 → `application-test.yml`에 datasource/redis URL을 명시할 필요 없음.
- 통합 테스트는 `MockMvcTester`(Spring Boot 4의 fluent assertion API) + AssertJ만 사용. 운영 흐름과 동일한 SecurityFilterChain을 거치도록 한다 — 권한 회귀를 통합 테스트가 잡는다.
- 테스트 파일 위치는 `test/.../api/<url>/<METHOD>_specs.kt`, 메서드명은 백틱 한국어 한 문장 (CLAUDE.md §5와 일관).

### 2. REST Docs → OpenAPI 3 → Swagger UI

- 통합 테스트의 happy path에 `MockMvcRestDocumentationWrapper.document("identifier")`를 적용 → `build/generated-snippets/<id>/`에 snippet 생성.
- `./gradlew openapi3`(restdocs-api-spec plugin)이 snippet을 읽어 `build/api-spec/openapi3.yaml` 합성. `./gradlew build`에 hook되어 빌드만 돌리면 spec이 항상 갱신됨 (거짓 문서 차단).
- bootJar는 spec을 `BOOT-INF/classes/static/api-spec/openapi3.yaml`로 박고, `swagger-ui` webjar + 정적 `swagger-ui.html` + `OpenApiSpecController`(classpath/filesystem fallback) 조합으로 `/swagger-ui.html`에 호스팅.

### 3. spec 호스팅이 controller를 거치는 이유

`copyOpenApiSpec` task를 main sourceSet output에 등록하면 `test → main classes → copyOpenApiSpec → openapi3 → test` circular dependency가 발생한다(openapi3가 test를 의존하므로). `bootJar`에만 직접 hook하고 bootRun은 controller가 `build/api-spec/openapi3.yaml`을 filesystem fallback으로 읽도록 분리.

## 검증된 호환성 함정

| 증상 | 원인 | 해결 |
|---|---|---|
| `@AutoConfigureRestDocs` import 안 됨 | Spring Boot 4.0에서 테스트 자동구성 모듈이 분리됨. autoconfigure가 `org.springframework.boot.<tech>.test.autoconfigure` 패키지로 이동. | `spring-boot-restdocs` (autoconfigure) + `spring-boot-webmvc-test` (`@AutoConfigureMockMvc`) + `spring-boot-testcontainers` (`@ServiceConnection`) 명시적으로 추가. |
| `Could not find org.testcontainers:postgresql:2.0.5` | Testcontainers 2.0부터 모든 모듈에 `testcontainers-` prefix 추가. Spring Boot 4 BOM이 박은 2.0.5와 1.x의 `org.testcontainers:postgresql` 좌표가 충돌. | `org.testcontainers:testcontainers-postgresql` / `org.testcontainers:testcontainers-junit-jupiter`로 좌표 변경 + `libs.versions.toml`의 `testcontainers`를 BOM 버전(2.0.5)에 정렬 (ADR-0011 패턴 재사용). |
| `ClassCastException: ReadOnlyHttpHeaders cannot be cast to Map` | restdocs-api-spec 0.19.4의 `BasicSecurityHandler`가 Spring Framework 7의 `HttpHeaders`를 Map으로 캐스팅. Spring 7부터 HttpHeaders가 Map을 implement하지 않음. | `restdocs-api-spec`을 0.20.1로 업그레이드 (Spring Boot 4 호환 fix 포함). |
| `OpenApi3Task: configuration cache 직렬화 실패` | restdocs-api-spec 0.20.1의 OpenApi3Task가 Jackson `StdDateFormat`을 들고 있고, Java 25의 strong encapsulation이 `java.text.DateFormat`의 reflection 접근을 거부. | task 단위 opt-out: `notCompatibleWithConfigurationCache(reason)`. plugin 호환 시 제거. |
| RT rotation이 같은 초에 통과 | `NimbusJwtIssuer`가 같은 sub + iat/exp로 토큰을 만들면 동일 JWT를 반환 → Redis의 활성 RT와 일치하여 두 번째 호출도 통과. | RFC 7519의 `jti` 클레임에 매 발급마다 UUID를 박아 토큰을 unique하게. 통합 테스트가 발견한 실제 보안 결함. |
| `:check ← :openapi3 ← :check` circular | restdocs-api-spec plugin이 `openapi3 → check` 의존성을 자체적으로 등록. 우리가 추가로 `check.dependsOn(openapi3)`를 박으면 순환. | `build.dependsOn(openapi3)`만 박는다. |
| `:resolveMainClassName uses :copyOpenApiSpec output without dependency` | spec 출력 디렉토리가 `build/resources/main`과 겹치면 Spring Boot plugin의 `resolveMainClassName`이 implicit 입력으로 잡아 Gradle 9 strict가 거부. | spec 출력을 `build/swagger-static`으로 분리하고 bootJar의 `from()`으로만 박음. |
| `test → main classes → copyOpenApiSpec → openapi3 → test` circular | sourceSets["main"].output.dir로 등록하면 main classes가 copyOpenApiSpec에 의존 → test가 main classes에 의존 → openapi3가 test에 의존 → 순환. | sourceSet 등록 제거. spec은 `OpenApiSpecController`가 런타임에 classpath/filesystem fallback으로 노출. |

## 결과 (ROI)

- 새 도메인 슬라이스 추가 시: `test/.../api/<url>/<METHOD>_specs.kt` 한 파일 + `document("identifier")` 한 줄 → `./gradlew build`만 돌리면 OpenAPI spec과 Swagger UI까지 자동 갱신.
- 운영 jar 한 개에 spec + UI가 함께 박혀 별도 배포물 없음.
- 통합 테스트가 실제 PostgreSQL/Redis 컨테이너 + 운영 SecurityFilterChain을 거치므로 권한/스키마/RT rotation 회귀를 빌드 시점에 차단.

## 거부된 옵션

- **Springdoc OpenAPI**: 컨트롤러 어노테이션을 스캔해 spec을 자동 생성. starter는 "테스트가 통과해야 문서 생성" 원칙을 박았으므로 거짓 문서가 가능한 경로를 차단하기 위해 거부.
- **MockMvc(legacy) + andDo()**: Spring Boot 4가 `MockMvcTester`를 표준 fluent API로 제시. starter는 cutting-edge 채택 원칙(ADR-0002)에 따라 새 API 사용.
- **bootJar의 sourceSet output 등록**: Gradle 9 strict + circular 회피 부담이 큼. controller fallback이 더 단순.
