# 0017. 테스트 인프라 — fixture + assertion 패턴 도입 (commerce-main 정수 추출)

- 일자: 2026-05-08
- 상태: Accepted
- 관련: ADR-0001 (헥사고날), ADR-0013 (통합 테스트 인프라)

## 배경

starter kit의 테스트는 Day 2까지 의도된 골격(`support/IntegrationTest`, `support/AuthTestHelper`,
`support/generator/Generators.kt`)으로 만들어졌지만 **도메인 객체 생성**과 **도메인 단언**의 일관 패턴이 없었다.

- 단위 테스트가 `Member.register(email, "hashed", now)`를 매번 직접 호출 → 같은 boilerplate가 흩어짐.
- AssertJ 단언이 필드별 `assertThat`으로 분산 → 의도(`isActiveWith(email)`, `hasTimestamps(now)`)가 한 줄에 박히지 않음.
- 죽은 코드: `support/ApplicationApiTest`(RANDOM_PORT 메타 어노테이션)가 `HealthApiTest.kt:17` 주석에서만 참조되고 실제 사용처 0 — 통합 테스트는 모두 `IntegrationTest`(MockMvc) 사용.

CLAUDE.md §5에 이미 "데이터: 매 테스트가 독립 데이터를 생성. `support/generator/` + `support/fixture/`. 단언: AssertJ. 도메인 단언은 `support/assertion/`의 확장 함수"가 박혀 있으나 `support/fixture/`와 `support/assertion/`이 코드로 구현되지 않음.

## 결정

### 1. commerce-main 정수 추출 — `TestFixture` + `*Assertions`

commerce-main은 도메인 객체 생성/검증을 다음 두 패턴으로 박았다.

- `TestFixture.create(env, repo)` + `createShopperThenIssueToken()` 같은 API 흐름 헬퍼.
- `ProductAssertions.isDerivedFrom(command)` + `JwtAssertions.conformsToJwtFormat()` 형태의 `ThrowingConsumer<T>`.

starter는 commerce의 골격을 Kotlin 친화 형태로 정수 추출한다.

- **API 흐름 헬퍼**는 starter에 이미 `AuthTestHelper`로 박혀 있으므로 추가하지 않는다(YAGNI). 새 도메인 추가 시 같은 패턴으로 `<Ctx>TestHelper`를 추가.
- **도메인 객체 fixture**는 `support/fixture/<Domain>Fixture.kt`로 박는다.
- **도메인 단언**은 `support/assertion/<Domain>Assertions.kt`로 박는다.

### 2. fixture 패턴 — `MemberFixture.member(...)`

```kotlin
object MemberFixture {
    fun member(
        email: Email = Email(generateEmail()),
        passwordHash: String = "hashed-pwd",
        now: OffsetDateTime = DEFAULT_NOW,
    ): Member = Member.register(email, passwordHash, now)

    fun memberInStatus(status: MemberStatus, ...): Member = ...
}
```

- default는 `EmailGenerator.generateEmail()`로 unique → 매 테스트가 독립 데이터(CLAUDE.md §5).
- 상태 전이는 도메인 행위 메서드(`deactivate`/`ban`)로만 — 캡슐화 우회 금지(§2). reflection으로 PENDING 상태를 만들지 않는다.
- `register` 팩토리가 ACTIVE로 시작하므로 PENDING 시나리오는 starter scope 외(주석으로 명시).

### 3. assertion 패턴 — `ThrowingConsumer<T>` (Kotlin extension 채택)

**거부 — `AbstractAssert<...>` 상속**:
- AssertJ 표준 패턴(IDE 자동완성 ↑, chaining `.hasEmail(...).isInStatus(...)` 가능).
- 그러나 Kotlin 계층 + Java AssertJ generic 상속이 더 대대적 — Kotlin starter의 단순함 원칙(§2)과 어긋남.

**채택 — Kotlin object의 `ThrowingConsumer<T>` 팩토리**:
```kotlin
object MemberAssertions {
    fun isActiveWith(email: Email): ThrowingConsumer<Member> = ThrowingConsumer { m ->
        assertThat(m.email).isEqualTo(email)
        assertThat(m.currentStatus).isEqualTo(MemberStatus.ACTIVE)
    }
    fun hasTimestamps(createdAt: OffsetDateTime, updatedAt: OffsetDateTime = createdAt) = ...
}

// 사용
assertThat(member).satisfies(
    MemberAssertions.isActiveWith(email),
    MemberAssertions.hasTimestamps(now),
)
```

AssertJ의 `Object.satisfies(ThrowingConsumer<T>... consumers)` API와 자연 조합. 새 단언 추가 비용 ↓ (단순 fun 추가).

### 4. JWT 형식 단언 — `JwtAssertions.conformsToJwtFormat()`

decoder 없이 JWT 토큰 문자열의 형식 자체(3-segment + base64url-encoded JSON)를 검증한다. 이는 NimbusJwtIssuer가 발급한 토큰이 RFC 7519 기본 골격을 준수하는지를 단위 테스트에서 한 줄로 검증하게 한다. subject/claim 검증은 컨텍스트마다 decoder가 다르므로 호출처에서 직접 사용(NimbusJwtIssuerTest 패턴 유지).

### 5. 죽은 코드 정리 — `ApplicationApiTest` 제거

`support/ApplicationApiTest`(RANDOM_PORT 메타 어노테이션) 제거. 모든 통합 테스트가 `IntegrationTest`(MockMvc + Testcontainers)를 사용 — `ApplicationApiTest`는 `HealthApiTest.kt:17` 주석에서만 언급되는 stub이었다. `HealthApiTest` 주석은 `IntegrationTest`로 갱신.

## 함정

- **`ThrowingConsumer<T>`의 vararg + Kotlin SAM 변환**: AssertJ 3.x의 `ThrowingConsumer`는 `@FunctionalInterface`이지만 vararg generic 호출에서 Kotlin이 SAM 변환을 거부하는 경우가 있다 → 명시적 `ThrowingConsumer { ... }` 람다 사용. AssertJ Java API와 정합.
- **fixture에 reflection 시나리오 추가 유혹**: PENDING 상태를 fixture로 만들기 위해 reflection으로 status 필드를 직접 변경하지 않는다. 도메인 행위로만 만들 수 있는 상태가 fixture로도 만들 수 있는 상태이고, 이 경계가 도메인 모델 패턴(§2)을 지킨다.
- **fixture default 시점이 `LocalDateTime.now()` 같은 system clock**이면 결정론 깨짐. starter는 `OffsetDateTime.of(2026, 5, 7, ...)`로 고정된 default를 박았다. 시점 의존이 있는 테스트는 named arg로 `now =` 명시.

## 영향

- **MemberTest 5개 케이스가 `MemberAssertions` + `MemberFixture`로 갱신**되어 의도가 한 줄에 박힘 (`isActiveWith(email)`, `hasTimestamps(now)`).
- **NimbusJwtIssuerTest에 JWT 형식 단언 케이스 1개 추가** → 48 tests / 0 failures. JwtIssuer 어댑터의 RFC 7519 기본 정합 명시적 검증.
- **새 도메인 추가 시 default checklist**: `<Ctx>Fixture.kt` + `<Ctx>Assertions.kt` + (필요 시) `<Ctx>TestHelper.kt`. 일관 패턴 → fork된 새 사용자가 첫 도메인을 추가할 때 인프라 비용 ↓.
- **죽은 코드 -1**: ApplicationApiTest 제거. 미사용 메타 어노테이션이 fork된 사용자에게 잘못된 패턴 신호를 주는 것을 차단.

## 후속

- `support/assertion/`에 도메인이 추가되면 같은 파일 또는 별도 `<Ctx>Assertions.kt`로 단언을 박는다.
- `JwtAssertions`는 컨텍스트별 decoder가 필요한 `hasSubject`/`hasClaim` 같은 단언이 필요해질 때 별도 클래스(`JwtClaimAssertions(decoder)`)로 분리한다 — 현재는 starter scope 외.
- TestFixture 패턴이 commerce처럼 **API 통합 흐름**(register → token → request)으로 확장될 가능성: 이미 `AuthTestHelper`가 그 역할이므로 새 도메인이 같은 흐름이 필요해질 때만 일반화.
