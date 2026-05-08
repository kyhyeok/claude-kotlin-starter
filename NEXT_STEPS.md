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
- ✅ **Day 3-3 완료**: Micrometer + Prometheus + 도메인 메트릭. `micrometer-registry-prometheus` 의존성 추가(BOM 정렬). `/actuator/prometheus` permitAll(SecurityConfig) — 운영은 reverse proxy/IP 화이트리스트로 보호. `MetricRecorder` 포트(`application/required/`) + `MicrometerMetricRecorder` 어댑터(`adapter/observability/`)로 헥사고날 정합. `MemberRegistrationService`에 success/duplicate 카운터 호출 박음. 통합 테스트 3개 추가(`api/actuator/prometheus/GET_specs.kt` — 200 응답 + exposition 포맷 + `member_registration_total` 노출 검증). `./gradlew clean build` 33초 통과. ADR-0016 박제.
- ✅ **Day 4-1 완료**: 테스트 인프라 강화 — commerce-main 정수 추출. `support/fixture/MemberFixture.kt`(도메인 객체 생성, default unique email + 결정론 시점) + `support/assertion/{MemberAssertions,JwtAssertions}.kt`(`ThrowingConsumer<T>` + AssertJ `satisfies(...)`로 도메인 단언 한 줄에 박음). MemberTest 갱신 + NimbusJwtIssuerTest에 JWT 형식 단언 케이스 추가. 죽은 코드 `support/ApplicationApiTest.kt` 제거(미사용, HealthApiTest 주석에서만 참조). ADR-0017 박제. 48개 테스트 통과.
- ✅ **Day 4-2 완료**: starter scope 명시 + thin user 모델. 사용자 검토(2026-05-08)에서 Member 도메인이 starter 의도("가벼움 + 도메인 무관")를 넘어 깊게 박힌 것을 확인 → 4단계 정리. (1) `Email` VO + `MemberStatus` enum + 행위 메서드 + 도메인 예외 일부 제거(thin model: id/email/passwordHash/isActive/시점). (2) `application/member` 슬라이스 → Auth 편입. (3) 도메인 메트릭 sample 제거(`MetricRecorder` 포트 + `MicrometerMetricRecorder` 어댑터 + observability 디렉토리). (4) V1__init.sql 단순화(`status VARCHAR` → `is_active BOOLEAN`) + ADR-0018 박제(ADR-0012/0016 부분 supersede + starter scope 표 명시). 38 tests / 0 failures.

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

### Day 3-3. Micrometer + Prometheus + 도메인 메트릭 ✅

`micrometer-registry-prometheus` 의존성 추가(BOM 정렬, ADR-0011 패턴 재사용) + `/actuator/prometheus` permitAll(SecurityConfig) + `MetricRecorder` 포트(`application/required/`) + `MicrometerMetricRecorder` 어댑터(`adapter/observability/`) + `MemberRegistrationService`의 success/duplicate 카운터 호출. 통합 테스트 3개 추가(`api/actuator/prometheus/GET_specs.kt`). ADR-0016 박제.

**박힌 결정 요약**:
- `/actuator/prometheus` 보안: permitAll. 운영은 reverse proxy/IP 화이트리스트로 보호 — Prometheus scraper의 표준 운영 패턴(옵션 A).
- 도메인 카운터 의존: `MetricRecorder` 포트 분리 + Micrometer 어댑터(옵션 B). 헥사고날 단방향 의존(CLAUDE.md §1, §4) 유지. PasswordEncoder 직접 의존(ADR-0012)은 Spring Security 표준 인터페이스 예외였음.
- 카운터 명명: `<도메인>.<행위>` (`member.registration`) — Micrometer가 `member_registration_total`로 자동 변환(dot→underscore + `_total` 접미사).
- 태그 enum + 컴파일타임 분기 강제(`RegistrationResult.SUCCESS|DUPLICATE`).
- 카운터 호출 위치: 성공은 `members.save()` 후, 중복은 예외 throw 직전 — 메서드 정상 흐름과 분기 명확.

**검증된 함정 (ADR-0016에 영구 박제)**:
- `application.yml`의 expose 목록만으로는 부족 — `micrometer-registry-prometheus` classpath 부재 시 endpoint silent 404.
- SecurityConfig의 default authenticated가 `/actuator/prometheus`도 막음 → permitAll 명시 필요.
- Micrometer가 카운터 이름을 `member.registration` → `member_registration_total`로 변환(dot→underscore + `_total`).
- `SimpleMeterRegistry` cumulative — 같은 `@SpringBootTest` 컨텍스트의 다른 테스트가 register를 호출하면 카운터 누적. 정확 값 검증 시 `@DirtiesContext`/delta 필요. starter는 substring 매칭으로 충분.

### Day 4-1. 테스트 인프라 강화 (commerce-main 정수 추출) ✅

`support/fixture/MemberFixture.kt` + `support/assertion/{MemberAssertions,JwtAssertions}.kt` 도입. AssertJ `satisfies(...)`와 `ThrowingConsumer<T>`로 도메인 단언을 한 줄에 박음. ADR-0017 박제.

**박힌 결정 요약**:
- fixture: `<Ctx>Fixture.kt` 패턴. default는 unique 이메일 + 결정론 시점.
- assertion: Kotlin object + `ThrowingConsumer<T>` 채택. `AbstractAssert` 상속 거부.
- 죽은 코드 `ApplicationApiTest` 제거.
- 새 도메인 추가 시 default checklist: `<Ctx>Fixture.kt` + `<Ctx>Assertions.kt` + 필요 시 `<Ctx>TestHelper.kt`.

### Day 4-2. starter scope 명시 + thin user 모델 ✅

사용자 검토(2026-05-08)로 Member 도메인 깊이 정리. ADR-0018에 starter scope 표(박는 것/박지 않는 것) 영구 박제. ADR-0012/0016 부분 supersede.

**박힌 결정 요약**:
- starter scope: 도메인 무관 골격 + 인증 인프라 + **최소 user 모델**(id/email/passwordHash/isActive/시점)만 박는다. 상태 enum/행위 메서드/VO/도메인-bound 포트 시그니처는 fork된 서비스가 도메인 진화 시 추가.
- Member thin: Email VO 제거(Bean Validation `@Email @Size`로 대체), MemberStatus enum 제거(`isActive: Boolean`), activate/deactivate/ban 행위 제거, `MemberNotActive`/`MemberNotFound` 예외 제거.
- application/member 슬라이스 → Auth 편입 (`application/auth/MemberRegistrationService`).
- 도메인 메트릭 sample 제거 — `MetricRecorder` 포트 + `MicrometerMetricRecorder` 어댑터 + observability 디렉토리. Prometheus endpoint와 자동 메트릭은 유지(도메인 무관 인프라).
- V1__init.sql `status VARCHAR(20)` → `is_active BOOLEAN`. ADR-0009의 "이미 적용된 V 수정 금지"는 운영 보호 목적이라 starter는 운영 이전 단계 단순 수정 허용.
- 새 작업 도입 전 판단 룰: "도메인 무관 인프라인가, 아니면 sample 도메인에 한정되는가? 포트 시그니처에 도메인 단어가 박히지 않는가?"

### Day 3-4 / 후속. detekt 2.0 GA 모니터링 (외부 의존, 사용자 트리거 시에만)

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
| `/actuator/prometheus` 호출 시 404 (expose 설정은 있는데도) | `application.yml`의 `management.endpoints.web.exposure.include`에 `prometheus`가 있어도 `micrometer-registry-prometheus`가 classpath에 없으면 endpoint 자체 미등록 | `io.micrometer:micrometer-registry-prometheus` 의존성 추가 (BOM이 버전 관리, ADR-0011 패턴) |
| `/actuator/prometheus` 호출 시 401 | SecurityConfig가 default authenticated이고 actuator 경로별 permitAll 누락 | `.requestMatchers("/actuator/prometheus").permitAll()` 추가. 운영은 reverse proxy/IP 화이트리스트로 보호 (ADR-0016) |
| Micrometer 카운터 이름과 Prometheus 노출 이름 불일치 | Micrometer가 dot을 underscore로 치환 + `_total` 자동 부여. `member.registration` → `member_registration_total` | 통합 테스트는 변환된 이름으로 substring 검증. `_total`은 자동이므로 코드에 직접 박지 않음 |

## 결정 변경이 필요할 때

새 결정을 내리면 반드시:
1. 새 ADR 작성 (`adr/00NN-...md`) 또는 기존 ADR Superseded 처리
2. 코드 변경
3. README의 스택 표 갱신 (해당 시)
4. CHANGELOG에 기록 (Day 3 이후 도입 시)

거부된 ADR도 보존. 이력은 의사결정의 근거.
