# 이어서 작업하기

> 새 세션 시작 시 이 파일과 `README.md`를 먼저 읽으세요. ADR 0001~0013이 모든 핵심 결정의 근거입니다.

## 현재 상태 (2026-05-07)

- ✅ **Day 1 완료**: 빌드 검증 통과. Java 25 + Kotlin 2.3.21 + Spring Boot 4.0.6 호환성 검증.
- ✅ **Day 2-1 완료**: V1 마이그레이션(members 테이블) 작성 + DB 부팅 검증 통과. Spring Boot 4 + Flyway 11 autoconfig 모듈 분리 함정 해결(ADR — `spring-boot-starter-flyway` 채택).
- ✅ **Day 2-2 완료**: jOOQ codegen 활성화. DDLDatabase로 V*.sql을 직접 파싱(ADR-0010). jOOQ 버전을 Spring Boot BOM(3.19.32)에 정렬(ADR-0011). `./gradlew clean build` 통과.
- ✅ **Day 2-3 완료**: JWT 어댑터(NimbusJwtIssuer, RedisRefreshTokenStore) + Member 도메인 모델 + Auth Use Case 4개(register/login/refresh/logout) + AuthApi + ApiControllerAdvice 도메인 예외 매핑. ADR-0012 박제. 단위 테스트 26개 통과(`./gradlew clean build`).
- ✅ **Day 2-4 완료**: `@IntegrationTest` 메타 어노테이션(@ServiceConnection PostgreSQL/Redis + MockMvcTester + AutoConfigureRestDocs) + Health/Auth 통합 테스트 + REST Docs → OpenAPI 3 → Swagger UI(/swagger-ui.html). NimbusJwtIssuer에 `jti` 클레임 추가(통합 테스트가 발견한 RT rotation 결함 fix). ADR-0013 박제. `./gradlew clean build` 통과(44개 테스트).

## Day 2 — 4단계 분할 계획

진행 방식: 각 단계마다 `./gradlew clean build`로 검증. 실패 시 에러를 그대로 보고하며 수정 → 재검증.

### Day 2-1. Flyway 첫 마이그레이션 + DB 부팅 검증

- [ ] `src/main/resources/db/migration/V1__init.sql` 작성 (빈 골격, 또는 `members`/`refresh_tokens` 테이블)
- [ ] docker desktop 켜고 `./gradlew bootRun` → `compose.yaml`이 PostgreSQL/Redis 자동 기동
- [ ] Flyway가 V1 적용 → `flyway_schema_history` 테이블 확인
- [ ] `/health` 엔드포인트 호출 검증

**가능한 함정**: Flyway 11.10.0 + Hibernate 7 + Spring Boot 4 BOM 호환성. 첫 부팅 시 `ddl-auto: validate`가 빈 스키마 거부 가능 → V1 마이그레이션 필수.

### Day 2-2. jOOQ codegen 활성화 ✅

- [x] `build.gradle.kts`의 `jooq { ... }` 블록 활성화 — DDLDatabase 채택(ADR-0010, DB 의존 없이 V*.sql 직접 파싱)
- [x] `./gradlew jooqCodegen` → `build/generated/jooq/main/com/kim/starter/adapter/persistence/jooq/`에 `Members.kt`, `MembersRecord.kt`, `Public.kt`, `Keys.kt`, `Indexes.kt`, `Tables.kt` 생성
- [x] `./gradlew clean build` 통과 (compile + ktlint + 단위 테스트 + ArchUnit + bootJar)

**해결된 함정**:
- jOOQ Spring Boot BOM이 3.19.32로 다운그레이드 → codegen 버전을 BOM에 정렬(ADR-0011)
- Gradle 9 strict task validation: ktlint task가 generated 디렉토리 입력 의존성 요구 → `mustRunAfter("jooqCodegen") + dependsOn("jooqCodegen")`로 명시
- ktlint glob `exclude("**/generated/**")`가 srcDir 추가된 generated 디렉토리에 미매칭 → 절대 경로 lambda(`exclude { it.file.absolutePath.contains("/build/generated/") }`)로 강화

**컬럼 타입 매핑 검증** (Members.kt):
- `BIGSERIAL` → `Long?` + `BIGINT.identity(true)` ✓
- `VARCHAR(255)` → `String?` + length 보존 ✓
- `TIMESTAMPTZ` → `OffsetDateTime?` ✓
- COMMENT(한국어) → KDoc으로 보존 ✓

### Day 2-3. JWT 어댑터 + Auth API ✅

- [x] `adapter/security/JwtProperties.kt` (`@ConfigurationProperties`로 secret/TTL 통합 바인딩)
- [x] `adapter/security/NimbusJwtIssuer.kt` (HS256 + `typ` 클레임으로 AT/RT 구분)
- [x] `adapter/security/RedisRefreshTokenStore.kt` (`auth:refresh:{subject}` → RT, subject당 1개 활성)
- [x] `domain/member/{Email, MemberStatus, Member, MemberExceptions}.kt` (도메인 모델 패턴 시연)
- [x] `application/required/MemberRepository.kt` + `adapter/persistence/{MemberSpringDataRepository, JpaMemberRepository}.kt`
- [x] `application/member/provided/MemberRegister.kt` + `MemberRegistrationService.kt`
- [x] `application/auth/provided/{LoginAuthenticator, TokenRefresher, LogoutHandler}.kt` + `AuthenticationService.kt`
- [x] `adapter/webapi/auth/{AuthApi, AuthDtos}.kt` (register/login/refresh/logout, OAuth 2.0 응답 컨벤션)
- [x] `ApiControllerAdvice` 도메인 예외 매핑(Duplicate/InvalidCredential/MemberNotActive/MemberNotFound)
- [x] SecurityConfig: logout만 authenticated, 나머지 `/auth/**` permitAll
- [x] 단위 테스트 26개(`./gradlew clean build` 통과)
- [x] ADR-0012로 결정사항 박제

**박힌 결정 요약**:
- HS256 / Access 15분 / Refresh 7일 (ADR-0003).
- RT는 JWT(`typ=refresh`) + Redis rotation, subject당 1개 활성. JWT subject = `Member.id` String 표현.
- Member 영속성: JPA(`@Entity` 도메인 모델), Repository 포트 분리.
- PasswordEncoder는 application에서 직접 의존(starter 단순성).
- logout 본인 인증 필수(`@AuthenticationPrincipal Jwt`로 subject 추출).

### Day 2-4. 통합 테스트 + Testcontainers + REST Docs/Swagger ✅

- [x] `@IntegrationTest` 메타 어노테이션 — `@SpringBootTest + @AutoConfigureMockMvc + @AutoConfigureRestDocs + @ServiceConnection`(PostgreSQL/Redis Testcontainers).
- [x] `src/test/.../api/health/GET_specs.kt`, `auth/{register,login,refresh,logout}/POST_specs.kt` — MockMvcTester 기반 통합 테스트.
- [x] `application-test.yml` 정리 — `@ServiceConnection`이 datasource/redis URL 자동 주입.
- [x] `restdocs-api-spec` 0.20.1 업그레이드 (Spring Boot 4 `ReadOnlyHttpHeaders` cast 호환 fix).
- [x] `./gradlew build` → `build/api-spec/openapi3.yaml` 자동 생성 (snippet → OpenAPI 3 합성).
- [x] Swagger UI 호스팅 — `/swagger-ui.html` + `OpenApiSpecController` (classpath/filesystem fallback).
- [x] NimbusJwtIssuer에 `jti` 클레임 추가 — 같은 초에 같은 sub로 발급해도 토큰 unique → RT rotation 정상 작동.
- [x] ADR-0013 박제.

**해결된 함정** (ADR-0013에 영구 박제):
- Spring Boot 4 테스트 자동구성 모듈 분리 → `spring-boot-restdocs` + `spring-boot-webmvc-test` + `spring-boot-testcontainers` 명시적 추가.
- Testcontainers 2.0의 모듈 rename → `org.testcontainers:testcontainers-postgresql`/`testcontainers-junit-jupiter` 좌표 변경.
- restdocs-api-spec 0.19.4 ↔ Spring 7 `ReadOnlyHttpHeaders` cast → 0.20.1 업그레이드.
- `OpenApi3Task` ↔ Gradle 9 configuration cache (Java 25 strong encapsulation) → task 단위 opt-out.
- circular dep(`test ← copyOpenApiSpec ← openapi3 ← test`) → controller fallback으로 spec 노출, sourceSet output 미등록.
- RT rotation 결함 → `jti` UUID 클레임으로 토큰 unique 보장.

## Day 3 이후 (선택)

- [ ] `rename-package.sh` 스크립트 — 새 프로젝트 시작 시 `com.kim.starter` → `com.yourorg.yourapp`
- [ ] CI 강화 (Gradle wrapper validation, Codecov, Dependabot)
- [ ] Micrometer + Prometheus 검증
- [ ] detekt 2.0 GA 출시 모니터링 → 재도입 (ADR-0007 체크리스트 따름)

## 이어서 작업할 때 첫 5분 체크리스트

```bash
# 1. 디렉토리 진입
cd "/Users/kimyoonhyeok/QR 베이스가 될 프로젝트/spring-kotlin-starter"

# 2. 빌드가 여전히 통과하는지 검증 (의존성 캐시 만료 시 재해결)
./gradlew clean build

# 3. docker desktop 실행 확인 (Day 2-1 이후)
docker info

# 4. 어디까지 했는지 확인
git log --oneline | head -10
cat NEXT_STEPS.md
```

## 호환성 함정 빠른 참조 (Day 1/2 학습)

| 증상 | 원인 | 해결 |
|---|---|---|
| `compileJava (25) vs compileKotlin (24)` 불일치 | Kotlin 2.2.x JVM_25 미지원 | Kotlin 2.3.0+, `jvmTarget = JvmTarget.JVM_25` 명시 |
| `detekt was compiled with Kotlin 2.0.10` | detekt 1.23.x Kotlin 2.3 비호환 | detekt 임시 제거 (ADR-0007), GA 시 재도입 |
| `Plugin [id: 'X'] was not found` | alpha publish 누락 또는 plugin id 변경 | Maven Central 직접 좌표 확인, settings의 pluginManagement |
| `class-signature` ktlint 위반 | 생성자 인자 줄 분할 강제 | 줄 분할 + trailing comma, 또는 `./gradlew ktlintFormat` |
| ArchUnit `Couldn't import class` | ASM이 Java N bytecode 미지원 | ArchUnit 최신 patch 버전 (Java 25는 1.4.2+) |
| ArchUnit `Layer 'X' is empty` | ArchUnit 1.4.x strict | `.withOptionalLayers(true)` 옵트아웃 |
| Flyway가 부팅 시 적용 X (로그도 0줄) | Spring Boot 4에서 Flyway autoconfig가 별도 모듈(`spring-boot-flyway`)로 분리. `flyway-core`만 직접 명시하면 autoconfig 미활성 | `implementation(libs.spring.boot.starter.flyway)` 추가 (autoconfig+core 함께) |
| jOOQ 생성 코드가 `Unresolved reference 'VERSION_3_20'` 등으로 컴파일 실패 | Spring Boot 4 BOM이 jOOQ를 3.19.32로 박는데 codegen만 3.20.x를 쓰면 신규 API가 런타임에 없음 | jOOQ 버전을 BOM에 정렬 (ADR-0011) — `libs.versions.toml`의 `jooq`를 BOM 버전과 동기화 |
| `Task ':runKtlintCheckOverMainSourceSet' uses this output of task ':jooqCodegen' without declaring an explicit ... dependency` | Gradle 9 strict task validation. `sourceSets["main"].kotlin.srcDir(generated)` 등록 시 ktlint가 입력 의존성을 요구 | ktlint task에 `mustRunAfter("jooqCodegen") + dependsOn("jooqCodegen")` 명시 |
| ktlint가 jOOQ 생성 코드를 검사해서 `Property name should start with a lowercase letter` 발생 | ktlint `filter { exclude("**/generated/**") }`는 source set baseDir 기준 상대화. srcDir로 추가된 절대 경로 디렉토리에는 매칭 안 됨 | 절대 경로 lambda — `exclude { it.file.absolutePath.contains("/build/generated/") }` |
| `@AutoConfigureRestDocs`/`@AutoConfigureMockMvc`/`@ServiceConnection` import 안 됨 | Spring Boot 4.0에서 테스트 자동구성 모듈이 `spring-boot-restdocs`/`spring-boot-webmvc-test`/`spring-boot-testcontainers`로 분리. `spring-boot-test-autoconfigure`에서 빠짐 | 위 3개 모듈을 testImplementation에 명시적 추가 |
| `Could not find org.testcontainers:postgresql:2.0.5` | Testcontainers 2.0부터 모듈 이름에 `testcontainers-` prefix 추가 (Spring Boot 4 BOM이 박은 버전) | `org.testcontainers:testcontainers-postgresql` / `testcontainers-junit-jupiter` 좌표 사용 + `libs.versions.toml` 버전을 BOM에 정렬 (ADR-0011 패턴) |
| `ClassCastException: ReadOnlyHttpHeaders cannot be cast to Map` (REST Docs) | restdocs-api-spec 0.19.x의 `BasicSecurityHandler`가 Spring 7의 HttpHeaders를 Map으로 캐스팅. Spring 7부터 HttpHeaders가 Map 미구현 | `restdocs-api-spec` 0.20.1+로 업그레이드 |
| `OpenApi3Task: configuration cache 직렬화 실패` | restdocs-api-spec 0.20.x의 OpenApi3Task가 Jackson StdDateFormat을 들고 있고, Java 25 strong encapsulation이 `java.text.DateFormat` reflection을 거부 | task 단위 `notCompatibleWithConfigurationCache(reason)` opt-out |
| RT rotation이 같은 초에 통과 (보안 결함) | `NimbusJwtIssuer`가 같은 sub + iat/exp로 동일 JWT 반환 → Redis 활성 RT와 일치하여 두 번째 호출도 통과 | `jti` 클레임에 매 발급마다 UUID를 박아 토큰 unique 보장 |

## 결정 변경이 필요할 때

새 결정을 내리면 반드시:
1. 새 ADR 작성 (`adr/00NN-...md`) 또는 기존 ADR Superseded 처리
2. 코드 변경
3. README의 스택 표 갱신 (해당 시)
4. CHANGELOG에 기록 (Day 3 이후 도입 시)

거부된 ADR도 보존. 이력은 의사결정의 근거.
