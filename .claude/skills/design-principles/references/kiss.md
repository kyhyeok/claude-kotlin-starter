# KISS — Keep It Simple, Stupid

> **이 문서는 언제 읽나**: SKILL.md 결정 트리에서 "이 코드 너무 복잡한데", "재구현", "자체 필터/Adapter/Wrapper" 같은 신호가 잡혔을 때.
>
> **출처**: Kelly Johnson, Lockheed Skunk Works, 1960년대. 원문은 쉼표 없는 _"Keep it simple stupid"_.

"엔지니어가 멍청하다"는 뜻이 아니라 — _전장에서 평범한 정비공이 단순한 도구만으로 고칠 수 있어야 한다_는 의미였다. 소프트웨어로 번역하면: **단순한 해법이 복잡한 해법보다 낫다.** "영리해 보이는" 해법이 _이해하기 쉬운_ 해법보다 우선될 가치는 거의 없다.

> Einstein 보강:
> *"Everything should be made as simple as possible, but not simpler."*
> 단순함을 너무 밀면 _필요한 분기·검증·에러 처리_까지 빠진다. KISS는 "복잡함 회피"이지 "필수 처리 생략"이 아니다.

---

## 1. 통과 신호

- Spring Security OAuth2 Resource Server + Nimbus를 _그대로_ 신뢰한다 (자체 `JwtAuthFilter` 만들지 않음 — CLAUDE.md §6 명시 금지)
- Spring Data JPA `JpaRepository`를 정직하게 사용한다 — 어댑터 한 겹으로 도메인 친화 포트에 맞춘다 (불필요한 베이스 추상화 금지)
- Bean Validation(`@Valid` + `@NotNull`/`@Size`)과 도메인 VO `init { require(...) }`로 검증 완결
- 도메인 친화 예외 + `@RestControllerAdvice` + `ProblemDetail`만으로 에러 처리 완결
- 명확한 `for`/`forEach` 루프가 영리한 함수형 체이닝보다 낫다 (성능 문제가 _측정으로_ 확인되기 전엔)
- `MockK` + `springmockk`로 충분 — Mockito와 함께 쓰지 않는다 (CLAUDE.md §5 명시 금지)
- 통합 테스트는 `MockMvcTester` + Testcontainers 그대로 사용 (자체 HTTP 클라이언트 래퍼 만들지 않음)
- 횡단 관심사(캐싱·로깅·재시도)는 같은 포트의 _데코레이터로 합성_ — 비즈니스 로직과 섞지 않음 (CLAUDE.md §4)

## 2. 위반 신호

- 자체 `JwtAuthFilter` / 자체 토큰 파서 — Spring Security OAuth2 Resource Server를 우회 (🚨 CLAUDE.md §6 **명시 금지**)
- `JpaRepository`를 얇게 한 번 더 감싼 `BaseRepository<T, ID>` 추상화 (정당화 없음)
- Bean Validation으로 충분한 곳에 `Validator` 빈을 직접 만들고 reflection으로 검증
- `@Async` / `@Scheduled` / `@Transactional` 같은 표준 어노테이션 대신 직접 `ThreadPoolExecutor`·`TransactionTemplate` 보일러플레이트
- 한 메서드를 reified 제네릭 + 고차 함수 + sealed when으로 한 줄에 욱여넣어서 6개월 뒤 이해 불가
- "유연성"을 위해 `Map<String, Any?>` config로 받는 인자 (현재 호출자는 1개)
- Resilience4j / Spring Retry로 가능한 재시도/타임아웃을 처음부터 직접 구현
- `@RestControllerAdvice` + `ProblemDetail`로 가능한 에러 응답을 컨트롤러마다 try/catch로 직접 만듦
- Spring AutoConfig가 제공하는 `Clock` 빈 대신 자체 `TimeProvider` 추상화를 한 번 더 만든다

---

## 3. KISS의 한계 — 단순함은 공짜가 아니다

KISS를 _핑계로_ 다음을 생략하면 안 된다:

- 발생 _가능한_ 에러 처리 (YAGNI는 _발생 불가능한_ 시나리오에만 적용)
- 본질적 책임 분리 — 헥사고날의 `adapter → application → domain` 경계는 KISS와 충돌해도 유지 (CLAUDE.md §1)
- 사용자 입력 검증 (보안·안정성은 단순함보다 우선 — Bean Validation + VO `require`는 KISS와 양립)
- 캡슐화 — 도메인 `var` 외부 노출은 "단순해서"가 아니라 "위험해서" 막는다 (CLAUDE.md §2)
- 포트 분리 — `Clock` / `*Issuer` / `*Sender` 분리는 _현재_ 테스트 가능성 요구이므로 KISS와 양립

### KISS 정당화 체크

라이브러리·기존 패턴 위에 _새 추상화_를 얹기 _전_ 다음 질문:

1. 라이브러리 기본 동작으로는 정말 안 되는가? (먼저 시도해봤는가)
2. 이 추상화의 사용처가 _현재_ 2개 이상인가? (1개면 YAGNI 위반)
3. 추상화 이름만 보고 호출자가 동작을 정확히 추측할 수 있는가?
4. 6개월 뒤의 내가/팀원이 이 코드를 _바로_ 이해할 수 있는가?
5. 이 추상화가 헥사고날 의존 방향(`adapter → application → domain`)을 깨뜨리지 않는가?

세 가지 이상 _no_라면 추상화를 만들지 말고 라이브러리 기본 사용으로 돌아가라.

---

## 4. 관련 충돌

- **KISS ↔ DRY**: 추상화가 호출자의 이해 비용을 늘린다면 중복이 낫다 (KISS 우선)
- **KISS ↔ SRP** (좁은 범위): 분리 자체가 새 추상화·새 파일을 만든다면 KISS 우선
- **KISS ↔ SRP** (넓은 범위): 한 모듈이 여러 액터를 섬기게 되면 SRP 우선 — 헥사고날 경계는 본질적 분리

---

## 5. KISS 보강 격언 (필요 시 인용)

- Einstein: *"Everything should be made as simple as possible, but not simpler."*
- Antoine de Saint-Exupéry: *"Perfection is achieved not when there is nothing more to add, but when there is nothing left to take away."*
- Bjarne Stroustrup: *"Make simple tasks simple."*
