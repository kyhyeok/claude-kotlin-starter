---
name: commit
description: 변경사항을 확인하고 커밋 메시지를 작성하여 커밋 및 푸시합니다.
disable-model-invocation: true
allowed-tools: Bash AskUserQuestion
---

# Git 커밋 스킬

## 커밋 메시지 형식

```
type: 제목

본문 (선택사항)
```

## Type 종류

Conventional Commits 1.0.0 표준 준수.

- `feat`: 새로운 기능 추가
- `fix`: 버그 수정 (`hotfix/` 브랜치도 type은 `fix`)
- `docs`: 문서 변경
- `style`: 코드 형식 변경 (동작 변경 없음)
- `refactor`: 코드 구조 개선 (동작·외부 인터페이스 변경 없음)
- `perf`: 성능 개선
- `test`: 테스트 추가 또는 수정
- `build`: 빌드 시스템·외부 의존성 변경 (Gradle, Maven 등)
- `ci`: CI 설정·스크립트 변경 (GitHub Actions 등)
- `chore`: 위에 해당하지 않는 잡무 (설정 파일, 메타정보 등)
- `revert`: 이전 커밋 되돌리기

## 커밋 컨벤션

- type은 영어 소문자, 제목은 한국어로 작성
- 제목은 50자 이내
- 브랜치명에 지라 이슈 번호가 있으면 푸터에 기재
- **Breaking change는 type 뒤에 `!`** — 예: `feat!: 회원 ID 타입을 Long에서 UUID로 변경`
  또는 본문 푸터에 `BREAKING CHANGE: <설명>` 추가

## 실행 단계

1. `git status`, `git diff --stat`, `git branch --show-current`, `git fetch origin main`을 **동시에(병렬)** 실행
2. **main 최신 반영 확인**: `git log HEAD..origin/main --oneline`으로 확인
   - 반영되지 않은 커밋이 있으면 `git rebase origin/main` 실행
   - **충돌 발생 시**: `git rebase --abort`로 원복 후 사용자에게 안내하고 **커밋 중단**
3. 변경 내용을 분석하여 적절한 type과 한국어 제목 결정
4. 사용자에게 커밋 메시지 제안 및 확인 요청
5. 확인 후 스테이징(`git add`) 및 커밋 실행
6. `git push origin {브랜치명}` 실행

## 주의사항

- `.env`, `credentials`, 비밀키 등 민감한 파일은 커밋하지 않음
- 컴파일 확인은 CI에서 수행하므로 커밋 시 별도 확인하지 않음
