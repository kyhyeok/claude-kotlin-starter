---
name: review
description: 변경사항을 검수하고 코딩 컨벤션 위반, 버그, 테스트 누락을 확인한 뒤 커밋합니다.
allowed-tools: Read, Glob, Grep, Bash, AskUserQuestion, Skill
---

# 코드 리뷰 & 커밋 스킬

## 목표 시간: 2분 이내

병렬 실행을 최대한 활용하여 전체 프로세스를 2분 이내에 완료한다.

## 실행 단계

### 1단계: 변경사항 확인 + 컴파일 (병렬)

아래 명령을 **모두 동시에** 실행:
- `git status`
- `git diff --stat`
- `git diff` (변경 내용 상세)
- `./gradlew compileJava` (컴파일 검증)

컴파일 실패 시 → 오류 내용 보고 후 **즉시 중단**.

### 2단계: 변경 파일 대상 테스트 (선택적 실행)

**전체 테스트를 돌리지 않는다.** 변경된 파일과 관련된 테스트만 실행한다.

테스트 대상 선정 기준:
1. `git diff --name-only`로 변경된 `.java` 파일 목록 추출
2. 변경된 파일 중 Controller, Service 클래스를 식별
3. 해당 클래스에 대응하는 테스트 파일이 `src/test/` 에 존재하는지 확인
4. **존재하는 테스트만** `./gradlew test --tests "{패키지}.{TestClassName}"` 으로 실행
5. 대응하는 테스트 파일이 없으면 → 테스트 스킵하고 "테스트 파일 없음" 으로 보고

### 3단계: 코딩 컨벤션 검수

2단계와 **병렬로** 변경된 파일의 코드를 읽고 아래 체크리스트로 검수한다.
**변경된 파일만** 검수 대상이다. 기존 코드는 검수하지 않는다.

#### ⚠️ 최우선 검수: API 계약 보존
- 기존 API path가 변경되지 않았는지 확인
- 기존 Request/Response의 필드명, 타입이 변경되지 않았는지 확인
- 위반 시 **ERROR** 처리 (커밋 불가)

#### Controller
- 클래스 어노테이션: `@Slf4j`, `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor`, `@Tag`
- 메서드 어노테이션: `@Operation(summary = "...")`

#### Service
- 클래스 어노테이션: `@Slf4j`, `@Service`, `@RequiredArgsConstructor`
- 조회 메서드: `@Transactional(readOnly = true)`
- 변경 메서드: `@Transactional`
- 의존성 주입: `private final` 필드

#### Entity
- 직접 setter 사용 금지 → `update*()` 메서드
- 팩토리 메서드: `static of(...)` 또는 `static createFor...(...)` 사용

#### 예외 처리
- `CustomException` 사용 (일반 Exception throw 금지)
- 에러 enum에 상수 정의

#### 보안
- SQL Injection 가능성 (JOOQ 파라미터 바인딩 확인)
- 민감 정보 로깅 여부
- 인증/인가 처리 누락

### 4단계: 결과 보고

```
===== 코드 리뷰 결과 =====

변경 요약: {N}개 파일, +{N}줄 / -{N}줄

컴파일: 통과/실패
테스트: 통과/실패/스킵 (실행: {N}개)
API 계약: 보존/위반
컨벤션: {위반 항목 또는 "이상 없음"}

[ERROR/WARNING이 있으면 항목별 나열]
============================
```

### 5단계: 커밋

- ERROR가 없으면 → `/commit` 스킬을 호출하여 커밋 및 푸시 진행
- ERROR가 있으면 → 오류 내용 보고 후 수정 여부를 사용자에게 질문

## 심각도 기준

- **ERROR**: 컴파일 실패, 테스트 실패, **API path 변경**, **Request/Response 필드 변경**, 보안 취약점 → 커밋 불가
- **WARNING**: 컨벤션 위반, 누락된 어노테이션 → 사용자에게 보고 후 커밋 진행
- **INFO**: 개선 제안 → 보고만, 커밋에 영향 없음
