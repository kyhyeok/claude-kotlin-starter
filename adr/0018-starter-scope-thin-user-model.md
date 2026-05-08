# 0018. starter scope 명시 — thin user 모델 + 도메인 메트릭 sample 제거

- 일자: 2026-05-08
- 상태: Accepted
- 관련: ADR-0001 (헥사고날), ADR-0012 (Auth 슬라이스 — **부분 supersede**), ADR-0016 (도메인 메트릭 — **부분 supersede**), ADR-0017 (테스트 인프라 — fixture/assertion thin 대응)

## 배경

starter kit은 사용자가 **여러 서비스의 베이스**로 사용한다. Day 1~3-3에서 Auth/JWT/Member 슬라이스를 도입하면서 Member 도메인이 의도된 "공통 골격" 수준을 넘어 깊게 박혔다.

**우려 4가지** (사용자 검토 2026-05-08에서 명시):
1. Member 도메인이 인증용 최소 모델보다 훨씬 깊음(`Email` VO + 검증 패턴, `MemberStatus` enum 4개, `activate/deactivate/ban` 행위 메서드, 도메인 예외 4개) → fork 후 갈아엎는 비용 큼.
2. 도메인-bound 시그니처가 인프라에 침투(`MetricRecorder.recordMemberRegistration(RegistrationResult.SUCCESS|DUPLICATE)`) → "Member"라는 단어가 generic 인프라에 박힘.
3. fixture/assertion sample이 Member에 결합(`MemberFixture` / `MemberAssertions`) → 다른 도메인 fork 시 갈아엎음.
4. starter scope 경계가 ADR로 명시되지 않아 작업이 진행될수록 sample이 깊어지는 drift.

CLAUDE.md "도메인 무관 골격" 표현은 있으나, **무엇이 골격이고 무엇이 sample인가**의 경계 정의가 없었다.

## 결정

### 1. starter scope 명시

starter kit이 default로 박는 것 + 박지 않는 것을 ADR로 영구 박제한다.

| 박는다 (도메인 무관 인프라) | 박지 않는다 (도메인 sample 또는 fork 시 결정) |
|---|---|
| 헥사고날 골격(domain/application/adapter/config) | 도메인 모델의 행위 메서드, 상태 enum, VO |
| Java 25 + Kotlin 2.3 + Spring Boot 4 toolchain | 비활성/정지 회원 차단 같은 도메인 분기 |
| Flyway + jOOQ + DDLDatabase codegen | 도메인 메트릭(`<도메인>.<행위>` 카운터) |
| JWT 인증(SecurityConfig + Issuer/Decoder + RefreshTokenStore) | 도메인-bound 포트 시그니처(`recordMemberRegistration(...)`) |
| **최소 user 모델**(id, email, passwordHash, isActive, 시점) | 도메인 예외 풀세트 (`MemberNotActive` / `MemberNotFound` 등) |
| 통합 테스트 인프라(`@IntegrationTest` + Testcontainers + REST Docs) | API 흐름 헬퍼는 starter에 1개(`AuthTestHelper`)만 — 다른 도메인 추가 시 같은 패턴 |
| CI/Kover/Codecov/Dependabot | API 응답 DTO의 도메인-bound 필드 |
| `/actuator/prometheus` + 자동 메트릭(JVM/Hibernate/Hikari/HTTP) | observability 어댑터(`MicrometerMetricRecorder`) — fork 시 도메인 카운터와 함께 도입 |
| fixture/assertion 패턴 | `<Domain>Fixture` / `<Domain>Assertions` — 도메인 추가 시 함께 |

**판단 룰**: 새 작업 도입 전 항상 묻는다 — "이게 도메인 무관 인프라인가, 아니면 sample 도메인에 한정되는가? 포트 시그니처에 도메인 단어가 박히지 않는가?"

### 2. Member 도메인 thin 축소 (ADR-0012 부분 supersede)

```kotlin
@Entity @Table(name = "members")
class Member private constructor(
    val id: Long? = null,
    val email: String,
    val passwordHash: String,
    val isActive: Boolean = true,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
```

**제거**:
- `Email` value class + `init { require(...) }` 검증 → 이메일 형식·길이 검증은 어댑터 DTO Bean Validation(`@Email @Size(max=255)`)이 담당. 도메인은 단순 String.
- `MemberStatus` enum 4개(PENDING/ACTIVE/INACTIVE/BANNED) → `isActive: Boolean`. 상태 분기가 필요한 fork는 별도 컬럼/enum을 V2 마이그레이션에 추가.
- `Member.activate/deactivate/ban` 행위 메서드 → 도메인 행위 시연은 starter scope 외.
- `MemberNotFoundException`, `MemberNotActiveException` → fork에서 추가.

**유지**:
- `DuplicateEmailException`, `InvalidCredentialException` (Auth 흐름의 필수 분기).
- JPA `@Entity` + private constructor + companion object factory(`register`) — 캡슐화 패턴(CLAUDE.md §2)은 starter scope 골격.

**ADR-0012 supersede 범위**: Member 도메인의 풀세트 splearn 스타일 시연(상태 enum + 행위 메서드 + VO) 부분만. Auth 슬라이스 자체, RT rotation, JWT subject = Member.id 정책, PasswordEncoder 직접 의존은 그대로 유지.

### 3. application/member 슬라이스 → Auth 편입

`application/member/`(슬라이스 디렉토리) → `application/auth/`로 통합. `MemberRegistrationService` + `MemberRegister`(provided 포트)는 Auth 슬라이스의 register 흐름으로 박힌다. 별도 슬라이스는 starter scope 외 — fork된 서비스가 회원 컨텍스트가 더 깊어지면 그때 분리.

### 4. 도메인 메트릭 sample 제거 (ADR-0016 부분 supersede)

- `application/required/MetricRecorder.kt` 제거.
- `adapter/observability/MicrometerMetricRecorder.kt` + `adapter/observability/` 디렉토리 제거.
- `MemberRegistrationService`에서 `metrics.recordMemberRegistration(...)` 호출 제거.
- `api/actuator/prometheus/GET_specs.kt`의 `member_registration_total` 노출 검증 케이스 제거.

**유지**:
- `micrometer-registry-prometheus` 의존성 + `/actuator/prometheus` permitAll + 자동 메트릭(JVM/Hibernate/Hikari/HTTP) — 도메인 무관 인프라.
- ADR-0016의 supersede 범위는 "도메인 카운터 sample 부분"만. Prometheus 엔드포인트 활성화 + Security 정책은 유지.

**MetricRecorder generic API로 유지하는 안 거부 이유**: 사용처가 없는 포트는 죽은 인터페이스(YAGNI). fork된 서비스가 도메인 메트릭이 필요해질 때 generic 또는 도메인-bound 포트를 그때 결정.

### 5. V1__init.sql 단순화 + ADR-0009 정합

```sql
CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

`status VARCHAR(20)` + `idx_members_status` 제거. ADR-0009의 "이미 적용된 마이그레이션 수정·삭제 금지"는 **운영 환경 보호 목적**이므로 starter scope에는 다음과 같이 정합:

- starter kit은 운영 배포 이전 단계 → V1 직접 수정 허용.
- fork된 서비스는 fork 시점 이후 V1을 기반으로 운영 적용 시작 → 이후 V1 수정 금지(V2+ ALTER만).

이 정신은 ADR-0018 본 문서로 영구 박제.

### 6. fixture/assertion thin 대응 (ADR-0017 갱신)

`MemberFixture.memberInStatus` 제거(상태 enum 없음), `MemberAssertions.isInStatus` 제거(`isActive(): Boolean` 단언으로 대체). `Email` VO 의존 모두 String으로 갈음. ADR-0017의 fixture/assertion 패턴 자체는 그대로 유지.

## 함정

| 증상 | 원인 | 해결 |
|---|---|---|
| `MemberStatus` 사용처가 fork 후 V2 마이그레이션 추가 시 누락 | starter는 enum 자체가 없으므로 도메인 갱신 + V2 SQL을 같이 박아야 함 | fork된 서비스의 ADR로 status enum 도입 + V2 마이그레이션 동기화 |
| starter의 `Member`에 추가 컬럼 박을 때 V1을 또 수정하려는 유혹 | ADR-0009의 "이미 적용된 V 수정 금지"는 운영 보호이지만 starter scope는 운영 이전이라 회색 | 사용자 결정: fork 시점이 명확한 cutoff. starter 자체는 V1 직접 수정 OK, fork된 서비스는 V2+ ALTER만 |
| Bean Validation `@Email`이 starter의 형식 검증 충분한지 | RFC 5322 완벽 준수는 아님 | starter scope: jakarta validation의 `@Email` default로 충분. 더 엄격한 도메인 정책은 fork 시 도입 |
| fork된 서비스가 `application/auth/UserRegistrar`로 이름 변경하고 싶을 때 | starter는 `MemberRegistrationService` 명명을 유지(thin model이라도 회원 등록 서비스의 의미는 명확) | 단순 rename은 fork 시점에 IDE refactor — starter scope 외 |

## 영향

- **빌드**: `./gradlew clean build` 통과. **38 tests / 0 failures** (이전 48 → 39 → 38). 단위/통합 테스트가 thin model 정합으로 갱신됨.
- **삭제된 파일** (8개): `Email.kt`, `MemberStatus.kt`, `EmailTest.kt`, `MetricRecorder.kt`, `MicrometerMetricRecorder.kt`, `application/member/` 디렉토리, `adapter/observability/` 디렉토리, prometheus 카운터 케이스.
- **추가/축소된 의존성**: 없음. Micrometer Prometheus 의존성 자체는 유지(자동 메트릭 노출 위해).
- **fork된 서비스가 새 도메인을 추가할 때**: 정합된 default checklist (ADR-0017 갱신) — `<Ctx>Fixture.kt` + `<Ctx>Assertions.kt` + 필요 시 `<Ctx>TestHelper.kt`. 도메인 메트릭이 필요해지면 그때 `MetricRecorder` 포트 + 어댑터 도입.

## 후속

- 다음 작업/sample 도입 시 항상 §1 판단 룰을 먼저 적용 — 도메인 단어가 포트 시그니처에 침투하지 않는지, 사용처 없는 포트가 starter에 박히지 않는지.
- ADR-0007(detekt 보류)와 함께 starter scope의 "유지 보수 정책"을 형성 — 외부 의존 GA 미출시는 보류, 도메인-bound는 fork 후 결정.
- starter README의 "스택"/"다음 작업" 표는 새 ADR 박힐 때마다 cross-check (memory feedback §7 default).
