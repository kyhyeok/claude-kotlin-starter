---
name: test
description: 프로젝트 테스트를 실행하고 결과를 요약합니다.
allowed-tools: Bash, AskUserQuestion
---

# 테스트 실행 스킬

## 실행 명령

```bash
# 전체 테스트
./gradlew test

# 특정 클래스 테스트
./gradlew test --tests "{패키지}.{TestClassName}"

# 특정 패키지 테스트
./gradlew test --tests "{패키지}.*"
```

## 테스트 환경

- **프레임워크**: JUnit 5 (Jupiter)
- **프로필**: `test` (`@ActiveProfiles("test")`)
- **빌드 도구**: Gradle

## 결과 보고 형식

### 성공 시
```
===== 테스트 결과 =====
전체 테스트 통과
총 {N}개 테스트 실행, 소요시간: {T}초
======================
```

### 실패 시
```
===== 테스트 결과 =====
테스트 실패
- 성공: {N}개
- 실패: {N}개
- 스킵: {N}개

실패한 테스트:
1. {TestClassName}.{testMethodName}
   원인: {에러 메시지 요약}
======================
```

## 실행 단계

1. `./gradlew test` 실행 (인자가 있으면 `--tests` 옵션 추가)
2. 실행 결과 파싱
3. 위 형식으로 결과 요약 보고
4. 실패 시 사용자에게 수정 여부 확인
   - "실패한 테스트를 분석하고 수정하시겠습니까? (Y/N)"
   - Y 선택 시: 실패 원인 분석 후 수정 제안
