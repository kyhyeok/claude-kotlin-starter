# 이어서 작업하기

> 새 세션 시작 시 이 파일과 `README.md`를 먼저 읽으세요. ADR 0001~0013이 모든 핵심 결정의 근거입니다.

## 현재 상태 (2026-05-08)

- ✅ **Day 1 완료**: 빌드 검증 통과. Java 25 + Kotlin 2.3.21 + Spring Boot 4.0.6 호환성 검증.
- ✅ **Day 2-1 완료**: V1 마이그레이션(members 테이블) 작성 + DB 부팅 검증 통과. Spring Boot 4 + Flyway 11 autoconfig 모듈 분리 함정 해결(ADR — `spring-boot-starter-flyway` 채택).
- ✅ **Day 2-2 완료**: jOOQ codegen 활성화. DDLDatabase로 V*.sql을 직접 파싱(ADR-0010). jOOQ 버전을 Spring Boot BOM(3.19.32)에 정렬(ADR-0011). `./gradlew clean build` 통과.
- ✅ **Day 2-3 완료**: JWT 어댑터(NimbusJwtIssuer, RedisRefreshTokenStore) + Member 도메인 모델 + Auth Use Case 4개(register/login/refresh/logout) + AuthApi + ApiControllerAdvice 도메인 예외 매핑. ADR-0012 박제. 단위 테스트 26개 통과(`./gradlew clean build`).
- ✅ **Day 2-4 완료**: `@IntegrationTest` 메타 어노테이션(@ServiceConnection PostgreSQL/Redis + MockMvcTester + AutoConfigureRestDocs) + Health/Auth 통합 테스트 + REST Docs → OpenAPI 3 → Swagger UI(/swagger-ui.html). NimbusJwtIssuer에 `jti` 클레임 추가(통합 테스트가 발견한 RT rotation 결함 fix). ADR-0013 박제. `./gradlew clean build` 통과(44개 테스트).
- ✅ **Day 3-1 완료**: `scripts/rename-package.sh` — `<old> <new>` 명시 + dry-run + git cleanliness + BSD/GNU sed 분기 + bash 3.2 호환(`tr` 사용). *.kt/*.java/build.gradle.kts(group+jOOQ target+path 주석)/settings.gradle.kts/application*.yml 일괄 치환 + git mv 디렉토리 이동. end-to-end: 임시 cp → 스크립트 → `./gradlew clean build` 33초 통과(44개 테스트). ADR-0014 박제. README 5분 가이드 갱신.
- ✅ **Day 3-2 완료**: CI 강화 — `concurrency.cancel-in-progress` + `permissions: contents: read` + `validate-wrappers: true` + 실패 시 test report artifact + Kover 0.9.8 도입(jOOQ 생성 코드 + Application 진입점 제외) + Codecov upload(`codecov-action@v5`, `fail_ci_if_error: false`) + Dependabot(gradle/github-actions weekly, Spring Boot/Kotlin/Testing 그룹 묶음). `./gradlew clean build koverXmlReport` 1분 19초 통과 — 44개 테스트 + `build/reports/kover/report.xml` 생성. ADR-0015 박제.

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

## Day 3 이후

### Day 3-1. `rename-package.sh` 스크립트 ✅

`scripts/rename-package.sh` 완성 + ADR-0014 박제 + README "5분 시작" 갱신. 자세한 내용은 ADR-0014 참고.

**박힌 결정 요약**:
- argv 형식: `<old> <new>` 명시 (자동 검출 거부 — fork된 starter의 ambiguous base 위험).
- 치환 범위: 코드 + 설정만 (`*.kt`, `*.java(package-info)`, `build.gradle.kts`, `settings.gradle.kts`, `src/**/application*.yml`). README/CLAUDE.md/ADR/NEXT_STEPS.md는 history 보존.
- BSD/GNU sed 분기: `case "$(uname)" in Darwin) sed -i '' ;; *) sed -i ;; esac`.
- bash 3.2 호환(macOS 기본): `mapfile` 미사용, `${VAR//./\/}` 대신 `printf | tr` 사용.
- group 라인은 패키지 prefix(`com.kim`) 케이스를 위해 별도 정규식으로 통째 교체.
- `git mv`로 디렉토리 히스토리 보존. 빈 부모는 `find -type d -empty -delete`.
- 잔존 검증은 `grep -F`(fixed string) — regex `.`이 `/`를 매칭하는 거짓 양성 방지.

**검증된 함정 (ADR-0014에 영구 박제)**:
- `mapfile: command not found` — macOS bash 3.2 (GPL 회피). 미사용으로 회피.
- `${VAR//./\/}`가 백슬래시 보존(`com\/kim\/starter`) — bash 3.2 quirk. `tr` 사용.
- ArchUnit 레이어 마킹용 `package-info.java`가 누락 — find pattern을 `\(*.kt -o *.java\)`로 확장.
- build.gradle.kts 주석의 path 형태 `com/kim/starter` 잔존 — sed에 path 패턴 추가.
- `group = "com.kim"`이 단순 sed로 안 잡힘 — group 라인 별도 정규식.

### Day 3-2. CI 강화 ✅

`.github/workflows/ci.yml` 강화 + Kover + Codecov + Dependabot 도입 완료. ADR-0015 참고.

**박힌 결정 요약**:
- `concurrency.cancel-in-progress: true` + `permissions: contents: read` (PR 비용 절감 + 최소권한)
- `gradle/actions/setup-gradle@v4`의 `validate-wrappers: true` (별도 wrapper-validation-action 거부)
- 실패 시 `actions/upload-artifact@v4`로 `build/reports/tests/` + `build/test-results/` 보존(7일)
- Kover 0.9.8 채택(Jacoco 거부 — Kotlin inline 정확도). Kotlin 2.3.21 호환 검증.
- jOOQ 생성 코드(`adapter.persistence.jooq.*`) + `StarterApplicationKt` coverage 제외
- Codecov: `codecov/codecov-action@v5` + `fail_ci_if_error: false` (coverage는 신호이지 게이트가 아님)
- `./gradlew build koverXmlReport` 단일 step 실행(분리 시 Testcontainers 2번 → CI 시간 2배)
- Dependabot: gradle + github-actions weekly(월요일 09:00 KST). Spring Boot/Kotlin/Testing 그룹 묶음 (BOM 정렬 패턴, ADR-0011 재사용)
- CodeQL은 보류 — GHAS 라이선스 의존, 새 프로젝트가 보유 시 추가

**검증된 함정 (ADR-0015에 영구 박제)**:
- `koverXmlReport`를 분리 step → CI 시간 2배 (test 두 번 실행).
- jOOQ 생성 코드가 srcDir로 등록되어 Kover가 자동 포함 → coverage 분모 부풀림. excludes filter 필수.
- Dependabot 개별 PR이 Spring Boot BOM PR보다 먼저 머지 시 빌드 깨짐 → group 패턴으로 묶음.
- Codecov 자체 장애 → `fail_ci_if_error: false`로 CI 게이트 분리.

### Day 3-3. Micrometer + Prometheus 검증 (다음 세션 우선순위) ⏳

**현재 상태**:
- ✅ `application.yml`의 `management.endpoints.web.exposure.include`에 `prometheus` 이미 노출.
- ❌ `io.micrometer:micrometer-registry-prometheus` 의존성 미추가 → 현재 `/actuator/prometheus` 호출 시 404.
- ❌ `SecurityConfig`가 `/actuator/health/**`만 permitAll → `/actuator/prometheus`는 401(인증 필요).
- ❌ 도메인 메트릭 sample 없음 (Spring/Hibernate/Hikari 기본만).

**4단계 분할 계획** (사용자 선호 패턴 — Day 2-4 / 3-1 / 3-2 동일):

#### 3-3-1. micrometer-registry-prometheus 의존성 추가

- `libs.versions.toml`에 `micrometer-registry-prometheus = { module = "io.micrometer:micrometer-registry-prometheus" }` (BOM 관리, 버전 미명시).
- `build.gradle.kts`의 `dependencies`에 `implementation(libs.micrometer.registry.prometheus)`.
- `./gradlew clean build` 통과 검증(회귀 0).

#### 3-3-2. SecurityConfig에 prometheus permitAll + 통합 테스트

**결정 필요**:
- 옵션 A: `/actuator/prometheus` permitAll. 운영은 reverse proxy/IP 화이트리스트로 보호. (Prometheus scraper는 토큰 보유 X — 표준 패턴)
- 옵션 B: 기본은 인증 필요. 운영에서 별도 ServiceAccount 토큰으로 scrape.
- 권장: **옵션 A** (sample/dev 친화 + 표준 운영 패턴). ADR-0016에 결정 근거 박제.

작업:
- `SecurityConfig`의 `/actuator/health/**` 라인 옆에 `/actuator/prometheus` 추가.
- `src/test/.../api/actuator/GET_specs.kt`(또는 `prometheus/`) 통합 테스트 — 200 응답 + Content-Type 검증(`text/plain` 또는 `application/openmetrics-text`).
- `./gradlew clean build` 통과.

#### 3-3-3. 도메인 카운터 sample — MemberRegistrationService

**결정 필요**:
- 옵션 A: `MeterRegistry`를 직접 의존(application 슬라이스). Spring 의존성 추가지만 단순. PasswordEncoder 직접 의존 패턴(ADR-0012)과 정합.
- 옵션 B: `MetricRecorder` 포트 분리 + `MicrometerMetricRecorder` 어댑터. 헥사고날 정합성 ↑, 단위 테스트 격리 ↑.
- 권장: **옵션 B** (CLAUDE.md §1 단방향 의존 + §4 외부는 모두 포트 뒤). PasswordEncoder는 Spring Security 표준이라 예외였지만 Micrometer는 어댑터화 비용 적음.

작업:
- `application/required/MetricRecorder.kt` (`countRegistration(result: RegistrationResult)` 등).
- `adapter/observability/MicrometerMetricRecorder.kt` — `MeterRegistry.counter("member.registration", "result", result.value)`.
- `MemberRegistrationService`에 카운터 호출 추가(성공: `result=success`, 중복: `result=duplicate`).
- 단위 테스트 갱신 + 통합 테스트로 `/actuator/prometheus`에 `member_registration_total` 노출 검증.

#### 3-3-4. ADR-0016 박제 + NEXT_STEPS.md 갱신

- `adr/0016-micrometer-prometheus-domain-metrics.md` — 위 결정 + 함정 박제.
- `NEXT_STEPS.md`의 Day 3-3 ✅ 처리 + Day 3-4 우선순위 갱신.

**예상 함정** (다음 세션이 만날 가능성):
- Spring Boot 4 actuator 모듈 분리 — `spring-boot-starter-actuator`가 web 부분을 별도 모듈로 분리했을 가능성. 현재 `libs.spring.boot.starter.actuator` 의존만 있으므로 endpoint web exposure가 안 될 수 있음. → 빌드/부팅 후 `/actuator/prometheus` 200 확인 필수.
- Prometheus exposition format — Spring Boot 4의 actuator는 `Accept` 헤더에 따라 `application/openmetrics-text; version=1.0.0` 또는 `text/plain; version=0.0.4` 응답. 통합 테스트는 둘 다 수용하도록 작성.
- Counter 인스턴스 캐싱 — `MeterRegistry.counter(name, tags...)`는 같은 (name+tags) 호출에 같은 인스턴스 반환. 어댑터에서 인스턴스 lookup 비용은 무시 가능하지만, 호출마다 새 Counter를 만들지 않도록 주의.
- `SimpleMeterRegistry`(테스트 기본)의 cumulative 동작 — 테스트가 여러 케이스를 같은 ApplicationContext에서 돌리면 카운터가 누적. 통합 테스트 격리는 `@DirtiesContext` 또는 카운터 delta 검증으로 해결.
- `/actuator/prometheus` 응답에 JVM/Hibernate/Hikari/HTTP 메트릭이 자동 노출 → 응답 본문이 큼(수십 KB). 단순 substring 검증("member_registration_total" 포함)으로 충분.

**검증 명령**:
```bash
./gradlew clean build koverXmlReport               # 회귀 + coverage
./gradlew bootRun                                  # 사용자 직접 실행 — ADR-0009
curl -s http://localhost:8080/actuator/prometheus | grep member_registration  # 카운터 노출 확인
```

**ADR 후보**:
- ADR-0016: Micrometer + Prometheus 도입 + MetricRecorder 포트 분리 + actuator 보안 정책.

### Day 3-4. detekt 2.0 GA 모니터링 (외부 의존)

- [ ] detekt 2.0 GA 출시 시 ADR-0007 체크리스트 재실행 → 재도입.
- 모니터링: https://github.com/detekt/detekt/releases — Kotlin 2.3 호환 GA 출시 시점.

## 이어서 작업할 때 첫 5분 체크리스트

```bash
# 1. 디렉토리 진입
cd "/Users/kimyoonhyeok/QR 베이스가 될 프로젝트/spring-kotlin-starter"

# 2. 빌드가 여전히 통과하는지 검증 (의존성 캐시 만료 시 재해결)
./gradlew clean build

# 3. docker desktop 실행 확인 (통합 테스트가 PostgreSQL/Redis Testcontainers를 띄움)
docker info

# 4. 어디까지 했는지 확인
git log --oneline | head -10
cat NEXT_STEPS.md
```

**Day 3 시작 시 추가 체크**:
```bash
# 5. Swagger UI 시각 확인 (선택, 사용자 직접 실행 — ADR-0009로 LLM 실행 금지)
./gradlew bootRun  # → http://localhost:8080/swagger-ui.html

# 6. 운영 jar 빌드 산출물 확인
ls build/libs/                                # spring-kotlin-starter-*.jar
unzip -l build/libs/*.jar | grep static       # swagger-ui.html + api-spec/openapi3.yaml
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
