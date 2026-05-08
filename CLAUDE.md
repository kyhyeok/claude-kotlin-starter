# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

LLM 행동 지침은 `.claude/rules/`에서 자동 로드. 자세한 가이드: @개발가이드.md · 결정 근거: @adr/

본 문서는 Kotlin·Spring 표준 컨벤션이 아닌 **이 프로젝트의 차별화 규칙**만 다룬다.

**전제:** 헥사고날 아키텍처 + 도메인 모델 패턴. 새 코드는 이 원칙에 정렬한다. 어긋나면 멈추고 정렬한 뒤에 진행한다.

---

## 1. 의존 방향은 단방향

**`adapter → application → domain`. 역방향 절대 금지.**

- 도메인은 Spring 의존 0, JPA 어노테이션만 허용.
- 포트 분리: `application/<ctx>/{provided,required}/` — 제공 Use Case vs 요구 외부 자원.
- 슬라이싱은 도메인 컨텍스트 단위. 기술 슬라이싱(`controller/`, `service/`)은 어댑터에서만.
- **YOU MUST** 새 패키지 추가 전 ArchUnit `ArchitectureTest` 통과 확인.

## 2. 캡슐화로 시작한다

**객체는 이름 있는 정적 팩토리로 만들고, 상태는 행위 메서드로만 바꾼다.**

- 엔티티 생성: `private constructor` + `companion object`의 동사형 팩토리(`register`, `prepare`).
- 상태 전이: `private var` + 행위 메서드(`activate`) + `check(...)` 가드.
- VO: 단일값은 `@JvmInline value class`, 복합값은 `data class`. 모두 `init { require(...) }`로 즉시 검증.
- 분기 3개 이상: `sealed interface` + `data class/data object`. `else` 두지 않아 컴파일러가 누락 강제.

자문: "외부에서 `var`를 직접 바꿀 수 있는가?" 그렇다면 행위 메서드로 옮긴다.

## 3. 이름이 책임을 말한다

**접미사가 책임을, 동사가 의도를 보여준다.**

- Use Case 인터페이스: `*Register` / `*Modifier` / `*Finder` / `*Remover` (단일 행위). 구현은 `*Service`. **`*ServiceImpl` 금지.**
- 포트: `*Repository` / `*Issuer` / `*Store` / `*Sender`.
- 컨트롤러: `*Api` (`*Controller` 아님).
- 조회: `find...`는 nullable, `get...`은 없으면 예외. 혼용 금지.
- 패키지: 도메인 컨텍스트 단수형(`member/`, `order/`).

## 4. 외부는 모두 포트 뒤로

**시간·자원·외부 호출은 인터페이스로 분리한다.**

- 생성자 주입 + `private val`만. `@Autowired` 필드/setter/`lateinit` 주입 금지(테스트 제외).
- `Clock` 주입. `LocalDateTime.now()` / `Instant.now()` 직접 호출 금지.
- Repository는 도메인 친화 인터페이스 + `JpaRepository` 구현 분리. `JpaRepository` 메서드가 도메인에 새지 않게.
- 외부 호출(`JwtIssuer`, `EmailSender`)도 같은 패턴. 어댑터는 `adapter/<tech>/`.
- 횡단 관심사(캐싱·로깅·재시도)는 같은 포트의 데코레이터로 합성. 비즈니스 로직과 섞지 않는다.

## 5. 테스트는 행위 단위

**파일 위치가 URL을 말하고, 이름이 의도를 말한다.**

- 위치: `test/.../api/<url>/<METHOD>_specs.kt`. 클래스명 백틱 사용.
- 메서드명: 백틱 한국어 한 문장. `given_when_then` 영문 명명 금지.
- AAA 분리: Arrange / Act / Assert를 줄바꿈으로 구분. 한 테스트 = 하나의 검증.
- 데이터: 매 테스트가 독립 데이터를 생성. `support/generator/` + `support/fixture/`. **하드코딩 PK(`findById(1L)`) 금지.**
- 단언: AssertJ. self-comparison(`x.a == x.a`) 금지. 도메인 단언은 `support/assertion/`의 확장 함수.
- **YOU MUST** Mockito 금지 — MockK + springmockk만. Stub(데이터) vs Mock(호출 검증) 의도 분리.
- 통합 테스트: `MockMvcTester` + Testcontainers. 단위 테스트: 순수 Kotlin, Spring 컨텍스트 없이.

## 6. 운영 사고를 막는 절대 금지

**아래는 예외 없이 막는다.**

- **YOU MUST** `ddl-auto: validate` 고정. `create`/`update` 금지. 스키마는 Flyway 단일 책임자.
- **YOU MUST** DB 적용 명령(`bootRun`/`flywayMigrate`/`psql`/임의 SQL) 직접 실행 금지. `db/migration/V*.sql` 파일 작성은 가능, 사람이 검토 후 적용. **이미 적용된** 마이그레이션 수정·삭제 금지 (ADR-0009).
- **YOU MUST** 시크릿은 환경변수만. `application.yml`에 평문 commit 금지.
- **YOU MUST** 자체 `JwtAuthFilter` 구현 금지. Spring Security OAuth2 Resource Server + Nimbus 사용.
- **YOU MUST** JWT subject는 안정적인 PK만. 변경 가능한 영문 식별자(loginId 등) 금지.
- 도메인 예외는 도메인 친화 타입(`MemberNotFoundException`)만. HTTP 매핑은 `@RestControllerAdvice`에서.
- TTL은 ISO-8601 Duration(`PT15M`, `P7D`).

## 7. 변경은 한 번에 한 변수씩

**빌드로 확인하기 전까지는 끝나지 않았다.**

```bash
./gradlew clean build                              # 컴파일 + 테스트 + ktlint
./gradlew test --tests "ClassName.methodName*"     # 단일 테스트
./gradlew ktlintFormat                             # 포매팅 자동 수정
./gradlew bootRun                                  # docker-compose 자동 기동
```

- 의존성/플러그인 변경은 한 번에 한 파일. 각 변경 후 `./gradlew clean build`로 검증.
- 정적 분석은 ktlint만. detekt는 ADR-0007에 따라 GA 출시 시 재도입.
- 비자명한 결정(라이브러리 버전 변경, 아키텍처 선택, 의존성 거부)은 새 ADR(`adr/00NN-...md`)로 박제. 거부된 결정도 보존.

## 8. 언어

코드 주석·커밋·문서·ADR은 **한국어**. 영어는 식별자와 외부 표준 용어만.

## 9. Git 워크플로우

**파생 프로젝트는 GitHub Flow + squash-merge.** 본 starter-kit 자체는 main-only(단일 작업자).

- 브랜치: `<type>/<topic>` 단기 브랜치 → PR → squash-merge → main. type 프리픽스: `feature` / `fix` / `hotfix` / `refactor` / `chore` / `docs` / `test`.
- 자동화 스킬: `commit` / `branch` / `pr`. 모두 `.claude/skills/`에 박제. 새 프로젝트는 즉시 사용 가능.
- 결정 근거: ADR-0008 · 사람용 가이드: `docs/git-workflow.md`.
- main 직접 push는 starter-kit 작업 한정. 파생 프로젝트는 GitHub repo의 브랜치 보호로 차단(설정 항목은 ADR-0008 참고).

## 10. 자기-검증 — architecture-reviewer

**큰 변경 후 commit 전에 SOLID·DRY·KISS·YAGNI + 헥사고날 규칙을 자기-검증한다.**

ArchUnit은 의존 방향 위반을 _빌드에서_ 잡지만, _의미 위반_(SRP 분쇄, 우연한 일치 추상화, `*ServiceImpl` 만능 서비스, 자체 `JwtAuthFilter` 같은 절대 금지 우회)은 못 잡는다. 이 갭을 `architecture-reviewer` 서브에이전트가 막는다.

- **자동 호출 대상** — 다음을 추가/수정한 직후, `./gradlew clean build` 통과 후 commit 전:
  - 새 도메인 슬라이스(`domain/<ctx>/`), 도메인 모델·VO·Exception
  - 새 Use Case(`application/<ctx>/provided/`) 또는 `*Service` 구현
  - 새 포트(`application/required/`) 또는 어댑터(`adapter/<tech>/`)
  - `SecurityConfig` · `ApiControllerAdvice` 등 횡단 어댑터
  - 새 통합 테스트(`api/<url>/<METHOD>_specs.kt`) 또는 `ArchitectureTest` 룰
- **흐름**:
  1. `./gradlew clean build` 통과 (ArchUnit + ktlint + 단위/통합 테스트)
  2. `architecture-reviewer` 호출 → 보고는 심각도별(🚨 높음 / ⚠️ 중간 / 💡 낮음 / 📌 범위 밖 / ✅ 잘된 점)
  3. 🚨 높음은 commit 전 수정. ⚠️ 중간은 ROI 판단. 💡 낮음·📌 범위 밖은 별도 PR/메모.
- **거부 (호출하지 않음)**:
  - 단순 오타·로그·이름 변경 같은 _외과적 변경_
  - `ktlintFormat` 자동 수정
  - 의존성 버전만 올리는 build 변경 (단, 새 모듈·플러그인 추가는 호출)
- **참조 문서**:
  - `.claude/agents/architecture-reviewer.md` — 에이전트 정의 + Self-check
  - `.claude/skills/design-principles/SKILL.md` — 정전 진입점 + §5 신호 빠른 점검
  - `docs/architecture-reference.md` — 이 프로젝트의 baseline 정답 파일 매핑

## 11. 주석은 WHY만, 짧게

**default = 주석 0. 식별자가 책임을 말한다.**

- 잘 명명된 함수/변수/타입은 추가 설명이 필요 없다. WHAT을 설명하는 주석은 코드와 중복이라 금지.
- 클래스/함수마다 KDoc 풀세트(`/** ... */`)를 박지 않는다. 정말 비자명한 정책만 한 줄.
- 주석을 쓸 때 기준:
  - 숨은 제약, 미묘한 invariant, 특정 버그 회피, 표준이 아닌 동작
  - 외부 결정 참조 한 줄: `// ADR-NNNN에 따라 ...` OK
- 금지:
  - WHAT 중복 (`// 회원을 등록한다`)
  - task-local 정보 ("added for X 흐름", "issue #123") → commit/PR 메시지로
  - 호출자/사용처 박기 → 코드 진화하면 stale

자문: "이 주석을 지우면 미래의 독자가 혼란할까?" → 아니면 지운다.

---

**이 가이드가 작동하면:** ArchUnit 위반 없는 PR, `var` 외부 노출 없는 도메인, 하드코딩 PK 없는 테스트, 평문 시크릿 없는 yml, 비자명한 결정은 자동으로 ADR에 박제되고, **새 도메인 슬라이스는 commit 전 architecture-reviewer로 자기-검증된다**.
