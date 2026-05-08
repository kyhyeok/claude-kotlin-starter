# 0016. Micrometer + Prometheus + 도메인 메트릭 포트 분리

- 일자: 2026-05-08
- 상태: Accepted
- 관련: ADR-0001 (헥사고날 — 외부는 모두 포트 뒤), ADR-0011 (BOM 정렬), ADR-0012 (Auth 슬라이스 — PasswordEncoder 직접 의존 예외)

## 배경

Day 3-2까지 CI/coverage/Dependabot은 박혔지만 **런타임 가시성**은 actuator 기본(`/actuator/health`)만 있었다. starter kit이 새 프로젝트의 기본값을 정의하는 만큼 다음 두 갭이 fork 직후부터 메워져야 한다.

1. **인프라 메트릭 노출 부재** — `application.yml`은 `prometheus`를 expose 목록에 등록했지만 `micrometer-registry-prometheus`가 classpath에 없어 endpoint가 silent 404. JVM/Hibernate/Hikari/HTTP 자동 메트릭이 운영 환경에서 보이지 않는다.
2. **도메인 시그널 부재** — Use Case가 일어나도 카운터가 없어 비즈니스 트래픽을 메트릭으로 추적 불가. "어떤 회원 등록이 중복 이메일로 거부되는가" 같은 질문이 로그 grep에 의존한다.

추가로 헥사고날 starter의 의존 단방향 원칙(CLAUDE.md §1, §4)을 깨지 않으려면 application 슬라이스가 `MeterRegistry`(io.micrometer.core 패키지) 같은 외부 라이브러리에 직접 결합되면 안 된다.

## 결정

### 1. Prometheus 레지스트리 의존성 추가 (BOM 정렬)

```toml
# libs.versions.toml
micrometer-registry-prometheus = { module = "io.micrometer:micrometer-registry-prometheus" }
```

```kotlin
// build.gradle.kts
implementation(libs.micrometer.registry.prometheus)
```

버전을 `[versions]`에 박지 않고 Spring Boot BOM 관리에 맡긴다 — ADR-0011(jOOQ/Testcontainers BOM 정렬) 패턴 재사용. Spring Boot가 박은 Micrometer 버전과 transitive 충돌이 생길 위험을 원천 차단.

### 2. `/actuator/prometheus` permitAll (옵션 A 채택)

```kotlin
// SecurityConfig
.requestMatchers("/actuator/prometheus").permitAll()
```

운영은 **reverse proxy/IP 화이트리스트**로 보호 — 사내 클러스터의 Prometheus scraper와 같은 네트워크 세그먼트로 격리. starter kit은 sample/dev 친화 + 표준 운영 패턴을 default로.

**거부 — 옵션 B (인증 필요)**:
- Prometheus scraper에 ServiceAccount 토큰을 주입하려면 별도 운영 절차(토큰 회전, 권한 분리)가 starter scope를 넘는다.
- permitAll이 운영 위험인 환경(공개 인터넷 노출 등)은 reverse proxy 단의 정책으로 막는 것이 현실 운영 패턴과 정합.
- 새 프로젝트가 강한 인증 요건을 가진 경우 SecurityConfig에서 한 줄 제거하면 됨 — 비대칭적 비용.

### 3. `MetricRecorder` 포트 분리 (옵션 B 채택)

```kotlin
// application/required/MetricRecorder.kt
interface MetricRecorder {
    fun recordMemberRegistration(result: RegistrationResult)

    enum class RegistrationResult(val tag: String) {
        SUCCESS("success"),
        DUPLICATE("duplicate"),
    }
}
```

```kotlin
// adapter/observability/MicrometerMetricRecorder.kt
@Component
class MicrometerMetricRecorder(
    private val meterRegistry: MeterRegistry,
) : MetricRecorder {
    override fun recordMemberRegistration(result: MetricRecorder.RegistrationResult) {
        meterRegistry.counter("member.registration", "result", result.tag).increment()
    }
}
```

application 계층은 `MetricRecorder` 인터페이스만 의존 — Micrometer/Spring 구체 의존 0.

**거부 — 옵션 A (`MeterRegistry` 직접 의존)**:
- 단순함은 ↑이지만 헥사고날 정합성 ↓. CLAUDE.md §1(단방향) + §4(외부는 모두 포트 뒤)와 어긋남.
- ADR-0012에서 `PasswordEncoder`는 직접 의존을 허용했지만 그것은 **Spring Security 표준 인터페이스**이기 때문(어댑터화 비용 ↔ 효용 비대칭). Micrometer의 `MeterRegistry`는 라이브러리 구체 타입이라 같은 예외 적용 불가.
- 카운터 호출 지점이 늘어날 때마다 어댑터 한 곳만 갱신하면 되는 구조 → 향후 OpenTelemetry/Datadog 전환 시 어댑터만 교체.

### 4. 카운터 명명 + 태그 컨벤션

- 카운터 이름: `member.registration` (도메인.행위) — Micrometer가 dot을 underscore로 치환해 Prometheus에서 `member_registration_total`로 노출. `_total` 접미사는 Micrometer 자동.
- 태그: `result=success|duplicate` (enum의 `tag` 필드로 컴파일타임 강제). `else` 없는 sealed/enum 분기로 누락 방지(CLAUDE.md §2).
- 새 행위가 추가되면 새 메서드를 `MetricRecorder`에 박는다. enum value 추가는 운영 대시보드 쿼리도 함께 갱신해야 함을 KDoc에 명시.

### 5. `MemberRegistrationService` 통합

```kotlin
override fun register(command: RegisterCommand): Member {
    if (members.existsByEmail(command.email)) {
        metrics.recordMemberRegistration(RegistrationResult.DUPLICATE)
        throw DuplicateEmailException(command.email)
    }
    // ...
    val saved = members.save(member)
    metrics.recordMemberRegistration(RegistrationResult.SUCCESS)
    return saved
}
```

성공 카운터는 `members.save()` 후 — DB 트랜잭션 commit 이전에 호출되지만 같은 트랜잭션 내 실패도 카운터에 반영되지 않도록 메서드 마지막 `return` 직전. 중복 카운터는 예외 throw 직전.

## 함정 (Day 3-3 검증으로 학습)

| 증상 | 원인 | 해결 |
|---|---|---|
| `/actuator/prometheus` 호출 시 404 | `application.yml`의 `management.endpoints.web.exposure.include`에 `prometheus`가 있어도 registry 의존성이 없으면 endpoint 자체가 등록 안 됨 | `io.micrometer:micrometer-registry-prometheus` 추가 |
| `/actuator/prometheus` 호출 시 401 | SecurityConfig에 명시 안 된 actuator 경로는 default authenticated | `.requestMatchers("/actuator/prometheus").permitAll()` |
| 통합 테스트에서 카운터 substring 매칭 실패 | Micrometer가 `member.registration` → `member_registration_total`로 변환. dot이 underscore가 되고 `_total` 자동 부여 | 테스트는 변환된 이름으로 검증 |
| `SimpleMeterRegistry` cumulative — 같은 컨텍스트에서 카운터 누적 | 통합 테스트가 `@SpringBootTest` 단일 컨텍스트를 공유 | substring 매칭(`contains("member_registration_total")`)으로 검증 — 정확 값 검증은 `@DirtiesContext` 또는 delta 측정 필요 |
| Counter 인스턴스 캐싱 | `MeterRegistry.counter(name, tags...)`는 같은 (name+tags) 조합에 같은 인스턴스 반환 | 호출마다 lookup해도 비용 무시 가능 — 명시적 캐싱 코드 불필요 |

## 영향

- **빌드**: `./gradlew clean build` 33초 통과(이전 1분 19초 대비 cache hit 증가). 통합 테스트는 Testcontainers 부팅 한 번 + 카운터 검증 케이스 추가.
- **새 프로젝트**: fork 직후 `./gradlew bootRun` → `curl /actuator/prometheus`로 즉시 메트릭 확인 가능. `member_registration_total{result="success"}` 같은 도메인 시그널이 Prometheus 대시보드에서 바로 가시화.
- **다음 도메인 추가 시**: `MetricRecorder`에 `record<도메인행위>(result: ...)` 메서드를 박고 어댑터에서 카운터 호출. 카운터 이름 컨벤션 `<도메인>.<행위>` 일관 유지.

## 후속

- 분포(latency)·게이지(현재 회원 수) 등 카운터 외 measurement type이 필요해지면 `MetricRecorder`에 메서드를 추가한다(`recordOrderProcessingLatency(duration: Duration)`). 어댑터는 `Timer`/`Gauge`로 매핑.
- OpenTelemetry 전환 검토 시 어댑터 교체로 단발 처리. application 계층은 무변경.
- 운영 환경의 reverse proxy/IP 화이트리스트 설정은 인프라 ADR로 별도 박제 (이 ADR scope 외).
