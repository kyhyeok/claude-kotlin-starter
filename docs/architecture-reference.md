# Architecture Reference

> starter kit의 **baseline 정답 파일 매핑**. 새 코드를 작성·리뷰할 때 "이 패턴은 어디서 정답?"을 즉시 찾는다.
>
> 정전(원칙 정의)은 `.claude/skills/design-principles/`에. 차별화 규칙은 `CLAUDE.md`에. 결정 근거는 `adr/`에.
> 본 문서는 **그 셋의 적용 사례**를 가리킨다.

---

## 1. 의존 방향과 패키지 구조

| 영역 | 패키지 | 기준 |
|---|---|---|
| 도메인 모델 | `domain/<ctx>/` | Spring 의존 0, JPA 어노테이션만 허용 |
| Use Case 인터페이스 (제공) | `application/<ctx>/provided/` | 단일 행위 (`*Register`, `*Modifier` 등) |
| 포트 인터페이스 (요구) | `application/required/` | `*Repository`, `*Issuer`, `*Store`, `*Sender` |
| Use Case 구현 | `application/<ctx>/*Service.kt` | `*ServiceImpl` 금지 (CLAUDE.md §3) |
| 인바운드 어댑터 | `adapter/webapi/<ctx>/*Api.kt` | `*Controller` 금지 |
| 아웃바운드 어댑터 | `adapter/<tech>/*` | `persistence/`, `security/` 등 |
| 횡단 Bean | `config/` | `Clock`, `Jackson` 등 |

ArchUnit 강제: `src/test/kotlin/com/kim/starter/ArchitectureTest.kt`

---

## 2. 도메인 모델 패턴

| 패턴 | 정답 파일 | 핵심 |
|---|---|---|
| 정적 팩토리 + `private constructor` | `domain/member/Member.kt` | `companion object`의 동사형 팩토리(`register`) |
| 상태 전이 (행위 메서드 + `check` 가드) | `domain/member/Member.kt` | `private var status` + `activate()` |
| Value Object (단일값) | `domain/member/Email.kt` | `@JvmInline value class` + `init { require(...) }` |
| 분기 `sealed` | `domain/member/MemberStatus.kt` | `else` 없이 컴파일러가 누락 강제 |
| 도메인 예외 (도메인 친화 타입) | `domain/member/MemberExceptions.kt` | HTTP 매핑은 `ApiControllerAdvice`에서 |

**확인**: 외부에서 `var`를 직접 바꿀 수 있는가? 그렇다면 행위 메서드로 옮긴다 (CLAUDE.md §2).

---

## 3. Use Case 슬라이스

| 영역 | 정답 파일 |
|---|---|
| Use Case 인터페이스 (단일 행위) | `application/member/provided/MemberRegister.kt` |
| Use Case 구현 (Service) | `application/member/MemberRegistrationService.kt` |
| 다중 행위가 한 도메인에 묶일 때 | `application/auth/provided/{LoginAuthenticator,TokenRefresher,LogoutHandler}.kt` + `AuthenticationService.kt` |
| 포트 (요구) | `application/required/MemberRepository.kt`, `JwtIssuer.kt`, `RefreshTokenStore.kt` |
| 응답 DTO (Use Case 경계) | `application/auth/TokenPair.kt` |

**확인**: Service 한 클래스가 여러 인터페이스(`LoginAuthenticator`, `TokenRefresher`, `LogoutHandler`)를 구현해도 OK. 인터페이스가 _액터별로 분리_되어 있으면 SRP/ISP 통과 (CLAUDE.md §3, ADR-0012).

---

## 4. 어댑터 패턴

| 어댑터 종류 | 정답 파일 |
|---|---|
| JPA Repository (도메인 친화 + Spring Data 분리) | `adapter/persistence/JpaMemberRepository.kt` + `MemberSpringDataRepository.kt` |
| JWT Issuer (Nimbus + `jti` UUID) | `adapter/security/NimbusJwtIssuer.kt` |
| Refresh Token Store (Redis, subject당 1 활성) | `adapter/security/RedisRefreshTokenStore.kt` |
| 컨트롤러 (인증·인가 흐름) | `adapter/webapi/auth/AuthApi.kt` |
| 컨트롤러 DTO | `adapter/webapi/auth/AuthDtos.kt` |
| Spring Security 설정 | `adapter/security/SecurityConfig.kt` |
| 도메인 예외 → HTTP 매핑 | `adapter/webapi/ApiControllerAdvice.kt` |
| OpenAPI spec 노출 (classpath/filesystem fallback) | `adapter/webapi/OpenApiSpecController.kt` |
| `@ConfigurationProperties` 바인딩 | `adapter/security/JwtProperties.kt` |

**확인**:
- `JpaRepository`는 Spring Data 인터페이스. _직접_ Use Case에 주입하지 말 것 — `MemberRepository`(요구 포트) 뒤로 감싼다.
- 자체 `JwtAuthFilter` 구현은 절대 금지. Spring Security OAuth2 Resource Server를 쓴다 (CLAUDE.md §6).

---

## 5. 테스트 baseline

### 단위 테스트 (Spring 컨텍스트 0)

| 영역 | 정답 파일 |
|---|---|
| 도메인 모델 | `domain/member/MemberTest.kt`, `EmailTest.kt` |
| Use Case (MockK + springmockk) | `application/member/MemberRegistrationServiceTest.kt`, `application/auth/AuthenticationServiceTest.kt` |
| 어댑터 단위 (Nimbus 시연) | `adapter/security/NimbusJwtIssuerTest.kt` |
| 컨트롤러 단위 (Clock DI 시연) | `adapter/webapi/HealthApiTest.kt` |

### 통합 테스트 (Testcontainers + MockMvcTester + REST Docs)

| 패턴 | 정답 파일 |
|---|---|
| 메타 어노테이션 (`@IntegrationTest`) | `support/IntegrationTest.kt` |
| Testcontainers PostgreSQL/Redis (`@ServiceConnection`) | `support/TestcontainersConfiguration.kt` |
| 테스트 데이터 생성 | `support/generator/Generators.kt` |
| 인증 토큰 헬퍼 | `support/AuthTestHelper.kt` |
| 외부 API 통합 테스트 위치 컨벤션 | `api/<url>/<METHOD>_specs.kt` |
| Health (단순 사례) | `api/health/GET_specs.kt` |
| 인증·인가 (REST Docs `document()` 포함) | `api/auth/{register,login,refresh,logout}/POST_specs.kt` |
| ArchUnit 의존 방향 검증 | `ArchitectureTest.kt` |

**확인**:
- 메서드명은 백틱 한국어 한 문장. `given_when_then` 영문 명명 금지.
- 데이터는 매 테스트가 독립 생성. 하드코딩 PK(`findById(1L)`) 금지.
- Mockito 금지 — MockK + springmockk만.

---

## 6. 횡단 인프라

| 영역 | 정답 파일 / 위치 |
|---|---|
| `Clock` 주입 (`LocalDateTime.now()` 금지) | `config/ClockConfig.kt` |
| 부트 클래스 + `@ConfigurationPropertiesScan` | `StarterApplication.kt` |
| Flyway 마이그레이션 (사람만 실행 — ADR-0009) | `src/main/resources/db/migration/V*.sql` |
| jOOQ codegen 설정 (DDLDatabase — ADR-0010) | `build.gradle.kts`의 `jooq { ... }` 블록 |
| jOOQ 버전 정책 (Spring Boot BOM 정렬 — ADR-0011) | `gradle/libs.versions.toml`의 `jooq` |
| OpenAPI/Swagger 호스팅 | `src/main/resources/static/swagger-ui.html` + `OpenApiSpecController` |
| 새 프로젝트 시작 (base 패키지 일괄 치환 — ADR-0014) | `scripts/rename-package.sh` |

---

## 7. 새 슬라이스 추가 절차

1. `domain/<ctx>/` — 도메인 모델·VO·Exception 작성 (Spring 의존 0)
2. `application/<ctx>/provided/<UseCase>.kt` — 단일 행위 인터페이스
3. `application/<ctx>/<UseCase>Service.kt` — 구현 (포트는 생성자 주입)
4. 필요한 포트가 없다면 `application/required/<Port>.kt` 추가
5. 어댑터: `adapter/<tech>/` — 포트 구현
6. `adapter/webapi/<ctx>/<Ctx>Api.kt` — 컨트롤러
7. 도메인 예외 추가 시 `ApiControllerAdvice`에 HTTP 매핑
8. 단위 테스트(`*Test.kt`) + 통합 테스트(`api/<url>/<METHOD>_specs.kt`) 작성
9. `./gradlew clean build` 통과
10. **`architecture-reviewer` 자기-검증** (CLAUDE.md §10) → 보고 검토 → 위반 수정
11. commit (한국어 메시지)

---

## 8. 패턴 선택 가이드 (혼동 방지)

- "조회만 한다 / 없으면 nullable" → `find...` (CLAUDE.md §3)
- "조회만 한다 / 없으면 예외" → `get...`. 혼용 금지.
- "쓰기 흐름 = 한 행위" → `*Register` / `*Modifier` / `*Remover` 인터페이스 분리
- "여러 행위가 한 도메인을 다룬다" → Auth 슬라이스처럼 인터페이스 3~4개 + Service 1개로 묶음 가능
- "JPA 쓰기 vs jOOQ 읽기" → 쓰기는 `JpaRepository`, 복잡한 조회는 jOOQ DSL
- "외부 호출(JWT, Email 등)" → 항상 포트(`*Issuer`/`*Sender`) 뒤로
- "TTL 표기" → ISO-8601 Duration(`PT15M`, `P7D`) (CLAUDE.md §6)
- "JWT subject" → 안정적인 PK만. 변경 가능한 loginId 등 금지 (CLAUDE.md §6)

---

## 9. architecture-reviewer가 자주 잡는 위반

`.claude/skills/design-principles/SKILL.md` §5의 starter kit 컨텍스트 인스턴스:

- `*Api`가 `JpaMemberRepository` 직접 주입 → `MemberRegister` Use Case 거치도록 변경
- `MemberRegistrationService`가 `passwordEncoder.encode(...)`를 도메인에 박아 넘김 → 도메인은 hashed 값을 받기만
- 도메인이 `LocalDateTime.now()` 직접 호출 → `Clock` 포트 주입
- 새 `*ServiceImpl` 네이밍 → `*Service`로 (CLAUDE.md §3)
- 사용처 1개인 `sealed` 추상화 → 인라인
- `JpaRepository`를 `application/required/`에 그대로 노출 → 도메인 친화 인터페이스로 감싸기
