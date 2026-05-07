# SOLID — 5원칙 상세

> **이 문서는 언제 읽나**: SKILL.md의 §3 결정 트리에서 SOLID 관련 신호가 잡혔을 때, 또는 PR 리뷰의 §5 "신호 빠른 점검"에서 SOLID 위반이 의심될 때.
>
> **출처**: Robert C. Martin, "Design Principles and Design Patterns" (2000), *Agile Software Development: Principles, Patterns, and Practices* (2003). 두문자어 정리 Michael Feathers.

SOLID는 Martin이 "나쁜 설계의 3대 증상"을 막기 위해 묶은 다섯 원칙이다:

- **Rigidity** (경직성) — 한 곳을 바꾸면 너무 많은 곳이 따라 바뀐다
- **Fragility** (취약성) — 한 곳을 바꾸면 엉뚱한 곳이 깨진다
- **Immobility** (부동성) — 다른 곳에 재사용하려 해도 떼어낼 수 없다

각 원칙은 _이 셋 중 어떤 증상을 막는가_의 관점에서 보면 더 잘 이해된다.

---

## 1.1 S — Single Responsibility Principle (SRP)

**정전 정의** (Robert C. Martin):
> *"A class should have one, and only one, reason to change."*
> "클래스는 변경되어야 할 이유를 단 하나만 가져야 한다."

**핵심 오해 정정**: SRP는 "한 가지 일만 한다"가 아니다. **"변경의 이유가 하나"**다. 즉 _누가_ 그 모듈의 변경을 요청하는가의 관점에서 본다 (회계팀이 보내는 변경과 인사팀이 보내는 변경이 같은 클래스에 떨어진다면 SRP 위반).

**막는 증상**: Rigidity. 책임이 섞여 있으면 한 책임의 변경이 다른 책임의 코드를 흔든다.

### 통과 신호

- 모듈이 _하나의 액터(역할/요청자)_의 변경 요구만 받는다
- "이 파일은 무엇을 하는가?"에 한 문장으로 답할 때 "and"가 들어가지 않는다
- 변경 요구가 들어왔을 때 영향받는 파일이 1~2개로 좁혀진다
- Use Case 인터페이스가 단일 행위(`*Register` / `*Modifier` / `*Finder` / `*Remover`)로 분리되어 있다 (CLAUDE.md §3)

### 위반 신호

- 한 `*Service`가 fetch + 변환 + 검증 + 외부호출 + 영속화 + 이벤트 발행을 모두 한다
- `*ServiceImpl` 네이밍 — 단일 행위로 쪼개지지 않은 만능 서비스의 신호 (CLAUDE.md §3 명시 금지)
- 한 도메인 엔티티가 영속화 형식 + 비즈니스 규칙 + 직렬화 응답을 모두 책임진다 (도메인 ↔ DTO 분리 부재)
- 파일 설명에 "그리고", "또한"이 두 번 이상 등장한다
- "이 파일을 누가 바꾸나?"에 답할 때 서로 다른 팀/도메인이 두 개 이상 나온다

### 예 — Kotlin/Spring

**위반**:
```kotlin
@RestController
class MemberController(
    private val jpaRepository: MemberJpaRepository,
    private val mailSender: JavaMailSender,
    private val passwordEncoder: PasswordEncoder,
) {
    @PostMapping("/api/members")
    fun register(@RequestBody req: RegisterRequest): ResponseEntity<MemberResponse> {
        // 1) 검증
        require(req.email.contains("@")) { "invalid email" }
        // 2) 중복 확인
        if (jpaRepository.existsByEmail(req.email)) error("dup")
        // 3) 비밀번호 해싱
        val hashed = passwordEncoder.encode(req.password)
        // 4) 영속화
        val saved = jpaRepository.save(MemberEntity(req.email, hashed))
        // 5) 환영 메일
        mailSender.send(buildWelcomeMail(saved.email))
        return ResponseEntity.ok(MemberResponse.from(saved))
    }
}
```
→ HTTP 변환 + 검증 + 도메인 규칙 + 인프라(영속화) + 외부호출(메일) 5가지 액터.

**개선** (헥사고날):
```kotlin
// adapter/inbound/web
@RestController
class MemberApi(private val register: MemberRegister) {
    @PostMapping("/api/members")
    fun register(@RequestBody req: RegisterRequest): ResponseEntity<MemberResponse> =
        ResponseEntity.ok(MemberResponse.from(register(req.toCommand())))
}

// application/member/provided
interface MemberRegister { operator fun invoke(cmd: RegisterMemberCommand): Member }

// application/member
@Service
class MemberRegisterService(
    private val memberRepository: MemberRepository,    // required port
    private val passwordHasher: PasswordHasher,        // required port
    private val welcomeMailer: WelcomeMailer,          // required port
) : MemberRegister {
    override fun invoke(cmd: RegisterMemberCommand): Member {
        val member = Member.register(cmd, passwordHasher) // 도메인 규칙은 도메인 안
        memberRepository.save(member)
        welcomeMailer.send(member.email)
        return member
    }
}
```
컨트롤러는 HTTP 변환만, Use Case는 오케스트레이션만, 도메인은 규칙만, 어댑터는 인프라만 — 액터별 분리.

### 과도 적용 금지

SRP를 극단으로 밀면 _Use Case 하나당 클래스 다섯 개_가 생기는 안티패턴이 나온다. SRP는 **응집도(cohesion)**의 도구이지 _분해(decomposition)_의 강박이 아니다. 한 동사(`register`)의 책임을 다섯 클래스로 쪼개고 한 곳에서 모두 주입받아 합치고 있다면 — 분리 자체가 잘못된 추상화일 수 있다.

---

## 1.2 O — Open/Closed Principle (OCP)

**정전 정의** (Bertrand Meyer 1988, Martin이 다듬음):
> *"Software entities should be open for extension, but closed for modification."*
> "소프트웨어 개체는 확장에는 열려 있어야 하고, 수정에는 닫혀 있어야 한다."

**의도**: 새 동작을 추가할 때 _이미 잘 동작하는 코드_를 건드리지 않고 새 코드를 _추가_하는 방식으로 구현하라. 기존 코드 수정은 회귀(regression) 위험을 만든다.

**막는 증상**: Fragility. 잘 돌아가던 곳을 건드려야 새 기능이 들어가는 구조라면 매번 새 버그가 생긴다.

### 통과 신호

- 새 동작 추가가 _기존 코드 수정_ 없이 _새 파일/클래스 추가_만으로 가능
- Spring 확장 포인트(`BeanPostProcessor`, `@Order`, `HandlerInterceptor`, `ControllerAdvice`, OAuth2 `JwtAuthenticationConverter` 등)를 사용한다
- 새 마이그레이션은 `V*.sql`을 _추가_만 한다 — 이미 적용된 파일은 절대 건드리지 않는다 (CLAUDE.md §6, ADR-0009)
- 횡단 관심사(캐싱·로깅·재시도)는 같은 포트의 _데코레이터로 합성_한다 (CLAUDE.md §4)
- 분기 3개 이상은 `sealed interface` + `data class`로 강제한다 — `else` 없이 컴파일러가 누락을 막는다 (CLAUDE.md §2)

### 위반 신호

- 새 케이스마다 기존 함수의 `when`/`if-else`에 분기를 추가하고 `else`에 throw를 넣어둔다 → sealed로 강제하지 않은 코드
- 이미 적용된 Flyway `V*.sql`을 _수정·삭제_한다 (ADR-0009 위반)
- openapi-generator 등 생성 코드 산출물을 직접 편집한다
- 한 포트 구현을 직접 수정해서 캐싱을 끼워 넣는다 (데코레이터로 합성하지 않고)

### YAGNI와의 함정 — 가장 자주 잘못 적용되는 부분

OCP는 _확장 포인트를 미리 만들어두라_는 뜻이 **아니다**. "미래에 다른 PG/SMS 게이트웨이가 올 수 있으니 Strategy 패턴으로 추상화하자" 같은 추측은 YAGNI 위반이다.

**규칙**: 확장 포인트는 _두 번째 사용처가 등장할 때_ 만든다. 한 가지 동작만 있는데 추상 인터페이스부터 짜지 마라. (단, 외부 자원에 대한 _포트 분리_는 다르다 — 그건 DIP 차원의 _현재_ 요구이지 OCP 차원의 미래 확장이 아니다.)

---

## 1.3 L — Liskov Substitution Principle (LSP)

**정전 정의** (Barbara Liskov 1987 OOPSLA, 후일 Liskov & Wing 1994년 형식화):
> *"Subtypes must be substitutable for their base types without altering the correctness of the program."*
> "하위 타입은 상위 타입의 자리에 들어가도 프로그램의 정확성을 깨지 않아야 한다."

**의도**: 상속/구현 계약을 _문법_ 수준이 아니라 _의미·행동_ 수준에서 지키라. 시그니처가 같다고 같은 타입이 아니다.

**막는 증상**: Fragility. 다형성을 쓰는 호출자가 _구체 타입에 따라_ 분기해야 한다면 다형성이 깨진 것이다.

### Kotlin/Spring 해석

- 포트 인터페이스(`*Repository` / `*Issuer` / `*Sender`)를 구현할 때, 구현체가 _상위 계약을 깨면서_ 던지는 예외 종류·전제 조건을 바꾸지 않아야 한다
- `JpaRepository`를 도메인 친화 인터페이스로 좁힐 때, 구현이 도메인 계약(`save`가 신규/갱신 모두 받는 의미)과 일치해야 한다
- Kotlin `sealed` 계층 — 호출자가 `when`으로 분기할 때 모든 분기가 _일관된 의미_를 가져야 한다 (한 분기만 throw로 막지 마라)
- 조회 메서드 네이밍은 의미와 일치해야 한다 — `find...`는 nullable, `get...`은 없으면 예외 (CLAUDE.md §3). 이 둘을 섞으면 호출자가 구현마다 다른 동작을 가정하게 된다

### 위반 신호

- `as` 다운캐스트 / `as? Foo ?: throw`로 sealed 계층 우회
- `!!` 남발로 nullable 계약을 무시
- 하위 타입이 상위 메서드를 `throw UnsupportedOperationException()`으로 막아버림
- 호출자가 `is` / `when (x) { is A -> ...; is B -> ... }`로 _특정 구체 타입에만_ 다른 동작을 강요당함
- `JpaRepository<T, ID>`를 인터페이스로 노출했는데 어떤 구현은 `delete`가 soft-delete이고 어떤 구현은 hard-delete (계약 모호)
- `find`/`get` 혼용 — 같은 인터페이스에서 어떤 메서드는 nullable, 어떤 메서드는 throw인데 명명 규칙이 어긋남

### 예 — Kotlin

**위반**:
```kotlin
interface PaymentGateway { fun charge(amount: Money): PaymentResult }

class PointGateway : PaymentGateway {
    override fun charge(amount: Money): PaymentResult =
        throw UnsupportedOperationException("포인트는 charge 불가") // ❌ 계약 파괴
}
```

**개선**:
```kotlin
interface PaymentGateway { fun charge(amount: Money): PaymentResult }
interface RewardGateway  { fun redeem(amount: Money): PaymentResult }

class PointGateway : RewardGateway { /* charge 계약을 약속하지 않는다 */ }
```

---

## 1.4 I — Interface Segregation Principle (ISP)

**정전 정의** (Robert C. Martin):
> *"Clients should not be forced to depend upon interfaces they do not use."*
> "클라이언트는 자신이 사용하지 않는 인터페이스에 의존하도록 강요받아서는 안 된다."

**의도**: 거대한 만능 인터페이스 하나보다 _역할 단위_로 작은 인터페이스 여러 개로 쪼갠다.

**막는 증상**: Rigidity. 만능 인터페이스의 한 메서드만 바뀌어도 그것을 안 쓰는 클라이언트까지 재컴파일/재배포된다.

### Kotlin/Spring 해석

- Use Case는 단일 행위로 쪼갠다 — `MemberFacade`(register/modify/find/remove 모두 노출) 대신 `MemberRegister` / `MemberModifier` / `MemberFinder` / `MemberRemover` (CLAUDE.md §3)
- 포트(`*Repository`)는 _도메인이 실제 쓰는 메서드_만 노출한다. `JpaRepository`의 모든 메서드(`findAll`, `deleteAllInBatch`, ...)가 도메인에 새지 않게 한다 (CLAUDE.md §4)
- 도메인 친화 인터페이스 + `JpaRepository` 구현 분리 — 도메인은 `MemberRepository.findByEmail(Email)`만 알고, `JpaRepository<MemberEntity, Long>`은 어댑터에만 있다

### 위반 신호

- 한 Use Case가 register/modify/find/remove 메서드를 모두 노출한다 (`*Facade` 안티패턴)
- 도메인 인터페이스가 `JpaRepository<T, ID>`를 그대로 상속/노출 → 호출자가 `findAll()` 같은 메서드까지 보게 됨
- 한 컨트롤러가 받는 Use Case 의존성이 5개 이상이고 핸들러마다 그중 1개만 쓴다

### 예 — Kotlin

**위반**:
```kotlin
interface MemberFacade {
    fun register(cmd: RegisterMemberCommand): Member
    fun modify(cmd: ModifyMemberCommand): Member
    fun find(id: MemberId): Member
    fun remove(id: MemberId)
    fun listAll(page: Pageable): Page<Member>
}
// 등록만 처리하는 가입 API가 modify/remove까지 의존하게 됨
```

**개선**:
```kotlin
interface MemberRegister  { operator fun invoke(cmd: RegisterMemberCommand): Member }
interface MemberModifier  { operator fun invoke(cmd: ModifyMemberCommand): Member }
interface MemberFinder    { operator fun invoke(id: MemberId): Member }
interface MemberRemover   { operator fun invoke(id: MemberId) }
```

### YAGNI와의 균형

인터페이스를 _미리_ 잘게 쪼개지 마라. 두 번째 클라이언트가 _덜 쓴다는 사실_이 드러났을 때 쪼갠다. (충돌 시 YAGNI 우선)
단, _액터별로 다른 행위_라는 사실이 처음부터 자명하면 처음부터 분리하는 것이 맞다 (CLAUDE.md의 단일 행위 규칙은 처음부터 적용 — 미래 확장이 아니라 _현재_ 액터 분리).

---

## 1.5 D — Dependency Inversion Principle (DIP)

**정전 정의** (Robert C. Martin):
> *"High-level modules should not depend on low-level modules. Both should depend on abstractions."*
> *"Abstractions should not depend on details. Details should depend on abstractions."*

**의도**: 고수준 정책(비즈니스 규칙·Use Case)이 저수준 세부(JPA, 외부 SDK, 메일 클라이언트, 시간)에 직접 의존하지 않게 한다. 둘 다 _도메인 추상화_에 의존시킨다.

**막는 증상**: Immobility. 저수준이 갈아치워질 때 고수준까지 함께 갈리는 것을 막는다.

### 헥사고날 해석 (이 프로젝트의 baseline — CLAUDE.md §1, §4)

```
adapter/inbound/web  (REST 컨트롤러 *Api)
       ↓ depends on
application/<ctx>/provided  (Use Case 인터페이스: *Register / *Modifier / ...)
       ↑ implements
application/<ctx>/<*Service>  (Use Case 구현)
       ↓ depends on
application/<ctx>/required   (포트: *Repository / *Issuer / *Sender / Clock)
       ↑ implements
adapter/outbound/<tech>      (JpaRepository 어댑터, JwtIssuer 어댑터, ...)

domain/<ctx>  (엔티티 / VO / 도메인 예외 — Spring 의존 0, JPA 어노테이션만 허용)
```

**역방향 절대 금지**. 어댑터→다른 도메인 어댑터 직접 호출도 금지 — 항상 application 포트를 거친다.

### 통과 신호

```kotlin
// ✅ 컨트롤러는 Use Case에만 의존
class MemberApi(private val register: MemberRegister) { ... }

// ✅ Use Case는 포트(추상)에만 의존
class MemberRegisterService(
    private val memberRepository: MemberRepository,  // 도메인 친화 포트
    private val clock: Clock,                        // 시간 추상
) : MemberRegister { ... }

// ✅ 포트는 application/required, 어댑터가 구현
package com.starter.application.member.required
interface MemberRepository {
    fun save(m: Member): Member
    fun findByEmail(e: Email): Member?
}

package com.starter.adapter.outbound.jpa
@Repository
class MemberRepositoryAdapter(
    private val jpa: MemberJpaRepository,
) : MemberRepository { ... }
```

### 위반 신호 — 거의 항상 🚨 높음

```kotlin
// ❌ 컨트롤러가 JpaRepository 직접 의존 — Use Case 우회
class MemberApi(private val jpa: MemberJpaRepository) { ... }

// ❌ Use Case가 JpaRepository에 직접 의존 (도메인 친화 포트 부재)
class MemberRegisterService(private val jpa: MemberJpaRepository) : MemberRegister { ... }

// ❌ 도메인이 Spring/인프라 의존
package com.starter.domain.member
@Component                                             // ❌ Spring 의존 (JPA 어노테이션만 허용)
class Member(...) {
    @Autowired lateinit var clock: Clock               // ❌ 도메인이 Spring 주입
    fun expireAt(): LocalDateTime = LocalDateTime.now() // ❌ Clock 미주입 (CLAUDE.md §4)
}

// ❌ 어댑터가 다른 도메인 어댑터 직접 호출 — application 우회
class MemberApi(private val orderJpa: OrderJpaRepository) { ... }

// ❌ @Autowired 필드/setter 주입
@Service
class MemberRegisterService : MemberRegister {
    @Autowired lateinit var memberRepository: MemberRepository  // 생성자 주입 우회 (CLAUDE.md §4)
}
```

이런 위반이 보이면: 적절한 application 포트를 통하도록 수정. 포트가 없다면 만들어야 한다 (단, 사용자 요청 범위 내일 때만). 도메인의 Spring 의존은 _즉시_ 제거 — ArchUnit `ArchitectureTest`가 막는다.

---

## 검증·해소 요약

| 신호 | 어느 원칙 | 심각도 |
|---|---|---|
| 컨트롤러/Use Case → JpaRepository/외부 SDK 직접 의존 | DIP | 🚨 높음 |
| 도메인이 Spring/인프라에 의존 | DIP | 🚨 높음 |
| `@Autowired` 필드/setter 주입 (생산 코드) | DIP/캡슐화 | 🚨 높음 |
| `LocalDateTime.now()` / `Instant.now()` 직접 호출 | DIP | 🚨 높음 |
| `as` 다운캐스트 / `!!` 남발 / sealed 우회 | LSP | 🚨 높음 |
| 이미 적용된 Flyway 마이그레이션 수정 | OCP | 🚨 높음 |
| 어댑터→다른 도메인 어댑터 직접 호출 | DIP | 🚨 높음 |
| 한 `*Service`가 5개 책임을 다 한다 / `*ServiceImpl` | SRP | ⚠️ 중간 |
| `*Facade` 만능 Use Case / `JpaRepository` 그대로 노출 | ISP | ⚠️ 중간 |
| "혹시" 미리 만든 Strategy/sealed 계층 | OCP (YAGNI 위반) | ⚠️ 중간 |
| 도메인이 `var` 외부 노출 / public 생성자 | SRP/캡슐화 | ⚠️ 중간 |
