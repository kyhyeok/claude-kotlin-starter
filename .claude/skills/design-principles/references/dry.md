# DRY — Don't Repeat Yourself

> **이 문서는 언제 읽나**: SKILL.md의 결정 트리에서 "중복 같은데", "헬퍼 추출", "공통화" 같은 신호가 잡혔을 때.
>
> **출처**: Andy Hunt & Dave Thomas, *The Pragmatic Programmer* (1999).

**정전 정의**:
> *"Every piece of knowledge must have a single, unambiguous, authoritative representation within a system."*
> "**모든 지식은 시스템 안에서 단일하고, 모호하지 않으며, 권위 있는 표현을 가져야 한다.**"

---

## 1. 핵심 오해 정정 — DRY는 "코드 중복"이 아니다

이 원칙이 가장 자주 잘못 적용된다. Hunt & Thomas는 명시적으로 다음과 같이 말한다:

> *"DRY is about the duplication of knowledge, of intent. It's about expressing the same thing in two different places, possibly in two totally different ways."*
> "DRY는 _지식·의도_ 의 중복에 대한 것이다. 똑같은 것을 두 곳에서 — 심지어 완전히 다른 방식으로 — 표현하는 것을 막는다."

따라서:

- 코드는 같은데 _지식이 다른_ 두 함수 → DRY로 묶으면 **안 된다** (우연한 일치)
- 코드는 다른데 _같은 비즈니스 규칙_을 표현하는 두 곳 → DRY 위반

### 우연한 일치의 예 — Kotlin

**겉보기엔 중복 — 묶으면 위험**:
```kotlin
fun calculateInvoiceTax(amount: Money): Money = amount * BigDecimal("0.1")
fun calculateShippingFee(weight: Weight): Money = Money(weight.kg * BigDecimal("0.1"))
```
같은 `* 0.1`이지만 한쪽은 _세율_, 한쪽은 _요율_. 세금 정책이 0.12로 바뀌면 한쪽만 영향받는다. _같은 코드가 같은 지식이 아니다._

**진짜 중복 — 묶어야 함**:
```kotlin
// MemberRegisterService.kt
require(email.contains("@") && email.length >= 5) { "invalid email" }
// AdminInviteService.kt
require(email.contains("@") && email.length >= 5) { "invalid email" }
```
같은 _이메일 검증 규칙_이라는 지식이 두 곳에 산다 → 단일 출처(`Email` VO의 `init { require(...) }`)로 통합:

```kotlin
@JvmInline
value class Email(val value: String) {
    init { require(value.contains("@") && value.length >= 5) { "invalid email" } }
}
```

---

## 2. 통과 신호

- 한 비즈니스 규칙(과세율, 검증 식, 정책 상수, 도메인 예외 분기)이 _한 곳_에만 정의됨
- VO(`Email`, `Money`, `MemberId`)가 단일 출처 — 모든 검증은 `init { require(...) }`에서 즉시 검증
- 정책 상수(TTL, 최대 재시도, 세율)가 도메인 또는 `application.yml` 한 곳에서만 선언 (TTL은 ISO-8601 — `PT15M`, `P7D`. CLAUDE.md §6)
- 도메인 친화 예외 타입(`MemberNotFoundException`)은 한 곳 정의 → 어댑터·Use Case·`@RestControllerAdvice` 어디서든 동일 타입 사용

## 3. 위반 신호

- 같은 검증 식(`email.contains("@") && length >= 5`)이 여러 Use Case에 산재 — VO로 모아라
- TTL/세율 같은 상수가 두 파일에 따로 선언
- 같은 도메인 규칙이 도메인 객체와 `*Service`에 _둘 다_ 적혀 있음 (도메인이 단일 출처여야 함)
- 같은 도메인 예외 매핑(HTTP 상태/응답 본문) 분기가 여러 컨트롤러에 흩어짐 → `@RestControllerAdvice`로 모아라
- DTO 검증을 Bean Validation 어노테이션으로 한 번, 도메인 VO `init`에서 또 한 번 _다른 식으로_ 적었다 (정책이 두 곳)
- 같은 SQL을 Flyway `V*.sql`과 코드(자체 DDL 실행)에 둘 다 적었다 (CLAUDE.md §6 `ddl-auto: validate` 위반 신호)

---

## 4. Rule of Three (Martin Fowler, *Refactoring* 1999)

> **2번까지는 중복 OK. 3번째 등장에서 추출.**

조기 추상화(premature abstraction)는 중복보다 비싸다. 두 번 등장한 시점에서는 그것이 _진짜로 같은 지식_인지 _우연히 비슷한 코드_인지 확신할 수 없기 때문.

```
1번째 등장: 그냥 쓴다
2번째 등장: 중복이라는 사실을 인지하고 표시(주석/TODO)만 한다
3번째 등장: 비로소 추출한다 — 이때 셋의 공통 패턴이 보인다
```

이 규칙을 어기는 흔한 패턴: 첫 번째 사용처에서 _이미_ "헬퍼/베이스 클래스로 빼자"고 결정 → 두 번째 사용처에서 모양이 미묘하게 달라서 옵션 매개변수 추가 → 세 번째 사용처에서 또 옵션 추가 → 결국 너덜너덜해진 추상화.

---

## 5. 안티패턴 — Hasty Abstraction (성급한 추상화)

> *"Duplication is far cheaper than the wrong abstraction."* — Sandi Metz, "The Wrong Abstraction" (2016)

"혹시 다른 도메인에서도 쓸지 모르니" 미리 만든 `BaseService` / `AbstractCrudController` / 제네릭 `Repository<T, ID>`. **지식이 일치한다는 증거가 없다면 만들지 마라.** 두 번째 사용처가 등장했을 때 _첫 번째와 모양이 미묘하게 다르면_ 결국 protected 훅 메서드와 타입 파라미터가 추가되며 너덜너덜해진다.

### 잘못된 추상화의 비용 (Metz)

1. 첫 사용자가 "거의 맞지만 약간 다른" 케이스를 가져옴
2. 추상화에 옵션 매개변수/제네릭 파라미터/protected 훅 추가
3. 다른 사용자가 또 다른 케이스 가져옴
4. 또 옵션 추가, 분기 추가
5. 결국 추상화 본문이 거대한 if-else 덩어리가 됨
6. 이제 _아무도_ 이 추상화를 이해하지 못한다

**탈출 전략**: 잘못된 추상화를 발견하면 — _되돌려서 인라인_하라. 중복 상태로 되돌린 뒤 _진짜 패턴_이 보일 때 다시 추출한다.

---

## 6. DRY 적용 시 자기 점검 질문

추출/공통화를 하기 _전_ 다음 질문을 해본다:

1. 두 코드의 _변경 이유_가 같은가? (= 같은 액터의 같은 요구로 함께 바뀌는가)
2. 호출자에게 추상화 이름만으로 동작이 정확히 추측되는가?
3. 옵션 매개변수 없이도 두 사용처를 모두 만족시킬 수 있는가?

세 질문 모두 _yes_가 아니라면 추출하지 마라. _yes_가 둘 이하면 Rule of Three까지 기다린다.

---

## 7. 관련 충돌

- **DRY ↔ YAGNI**: YAGNI 우선. 사용처가 1~2개면 추출하지 않는다
- **DRY ↔ KISS**: 추상화가 호출자의 _이해 비용_을 늘린다면 중복이 낫다
