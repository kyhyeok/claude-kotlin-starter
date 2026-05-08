# ADR-0012: Auth 슬라이스 도입 — Member 도메인 + 단일 행위 포트 + RT rotation

- 상태: Accepted (ADR-0018에 의해 일부 supersede — Auth 슬라이스 구조 유지, Member 도메인 thin 축소)
- 일시: 2026-05-07
- 관련: ADR-0001(헥사고날), ADR-0003(JWT OAuth2), ADR-0004(시크릿), ADR-0018(starter scope thin)

## 맥락

Day 2-3 진행 시점 starter kit에는 V1 `members` 마이그레이션과 JWT 포트(`JwtIssuer` / `RefreshTokenStore`)는
박혀 있었으나, 이를 사용하는 Use Case와 도메인 모델, HTTP API는 비어 있었다. Day 2-3에서 인증의
최소 풀세트(register/login/refresh/logout)를 starter에 도입하기로 결정하면서 다음 비자명한 결정 다섯 개가
함께 발생했고, 이를 한 ADR로 묶어 박제한다.

## 결정

### 1. Member 도메인 모델을 starter에 포함한다

V1 `members` 마이그레이션이 이미 박혀 있고, 인증의 최소 의존성 = Member이므로 도메인 모델 패턴
(CLAUDE.md §2)을 시연하는 표준 도메인으로 활용한다. `Member`는 `@Entity` + `private constructor` +
`Member.register` 동사형 팩토리 + 행위 메서드(`activate` / `deactivate` / `ban`) 구조.

`@Entity`(JPA 어노테이션)는 도메인에 허용한다(splearn 스타일, CLAUDE.md §1: "도메인은 Spring 의존 0,
JPA 어노테이션만 허용"). Spring 어노테이션은 절대 박지 않는다.

`Email`은 `@JvmInline value class` VO. Hibernate 7의 Kotlin value class `@Embeddable` 지원이 안정적이지
않아 영속 표현은 String 컬럼(`emailValue`) + `email: Email` getter로 노출.

### 2. Auth Use Case는 단일 행위 인터페이스 4개로 분할한다

CLAUDE.md §3("Use Case 인터페이스는 단일 행위") 시연을 위해:

- `application/member/provided/MemberRegister` — register
- `application/auth/provided/LoginAuthenticator` — login
- `application/auth/provided/TokenRefresher` — refresh
- `application/auth/provided/LogoutHandler` — logout

구현체는 컨텍스트 단위로 묶어 두 개:
- `MemberRegistrationService` (member 컨텍스트)
- `AuthenticationService` (auth 컨텍스트, 3개 인터페이스 구현)

### 3. Refresh Token = HS256 JWT + Redis rotation, subject당 1개 활성

- RT는 AT와 동일한 HS256 JWT로 발급하되 `typ="refresh"` 클레임으로 구분 → AT가 refresh 흐름에 사용되는
  것을 차단.
- Redis 키 스키마: `auth:refresh:{subject}` → 활성 RT 값. subject당 1개 → refresh 시 RT 신규 발급 +
  Redis 교체 → 이전 RT 자동 무효.
- TTL은 RT의 만료시각과 동기화 → 별도 cron 없이 Redis가 자동 정리.
- 멀티 디바이스 동시 로그인은 Day 3+ 후보(키에 deviceId/jti 분기).

### 4. logout은 본인 인증 필수

- `POST /auth/logout`만 `authenticated()`. 나머지 `/auth/**`은 `permitAll()`.
- 컨트롤러는 `@AuthenticationPrincipal Jwt`에서 subject를 추출하여 본인의 활성 RT만 폐기.
  → 클라이언트가 임의의 `memberId`를 폐기하는 경로 차단.

### 5. PasswordEncoder는 application에서 직접 의존

- Spring Security의 `PasswordEncoder` 표준을 application 서비스가 직접 생성자 주입.
- 포트 wrap(예: `PasswordHasher`) 없음 — 도메인은 이미 해시된 String만 받으므로 표준 의존이 단순.

## 결과

- starter kit fork 시 register/login/refresh/logout 풀세트가 즉시 동작.
- 도메인 예외(Duplicate / InvalidCredential / MemberNotActive) → HTTP 매핑은 `ApiControllerAdvice`
  단일 책임으로 처리. 도메인 코드는 의미만 박는다.
- 통합 테스트는 Day 2-4(Testcontainers + MockMvcTester)에서 추가.
- `register` 시 `MemberStatus.ACTIVE`로 시작 → 이메일 인증 단계가 필요한 도메인은 fork 후 `Member.register`
  팩토리를 PENDING으로 바꾸고 인증 완료 시 `activate()`를 호출하도록 흐름 갱신.

## 거부된 대안

- **RT = opaque random + Redis(token→subject) lookup**: 더 안전(서버가 무효화 100% 통제)하지만 JWT 인프라
  중복 활용 못 함. starter kit 단순성을 위해 JWT 채택. 보안 강화가 필요한 fork는 RT를 opaque로 교체.
- **PasswordHasher 포트 wrap**: 헥사고날 일관성은 좋으나 Spring Security 표준 PasswordEncoder를 한 번 더
  추상화하는 것은 starter kit에서 과잉. 비밀번호 해싱 정책 자체를 교체할 일은 드물다.
- **Member 영속성을 jOOQ로**: 단순 CRUD는 JPA가 자연스럽다. 복잡한 조회는 추후 jOOQ를 사용한 별도
  `MemberFinder` 포트로 추가(CLAUDE.md "쓰기는 JPA, 읽기는 jOOQ").
- **AuthService 단일 인터페이스에 4개 메서드 묶기**: 단일 행위 룰(CLAUDE.md §3) 시연 가치 손실.
- **logout을 RT 본문 검증으로**: 서버가 RT를 받아 폐기하는 흐름. 단순하지만 AT의 BearerToken 인증 흐름을
  활용하지 못해 일관성 ↓. 인증된 AT가 본인 식별 책임을 갖게 한다.
