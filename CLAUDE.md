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
- **YOU MUST** `db/migration/*.sql` 직접 작성·수정·삭제 금지. DB 스키마는 사람 단일 책임, LLM은 read-only. 스키마 변경은 권장 SQL만 텍스트로 제안. `bootRun`/`flywayMigrate`/`psql` 등 DB 영향 명령도 LLM이 실행하지 않음 (ADR-0009).
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

---

**이 가이드가 작동하면:** ArchUnit 위반 없는 PR, `var` 외부 노출 없는 도메인, 하드코딩 PK 없는 테스트, 평문 시크릿 없는 yml, 비자명한 결정은 자동으로 ADR에 박제된다.
