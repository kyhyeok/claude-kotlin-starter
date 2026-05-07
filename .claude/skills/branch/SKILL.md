---
name: branch
description: main 최신 상태에서 작업용 단기 브랜치를 분기하여 생성하고 원격 추적까지 설정합니다.
disable-model-invocation: true
allowed-tools: Bash AskUserQuestion
---

# Git 브랜치 생성 스킬

워크플로우 근거: `adr/0008-github-flow-squash-merge.md` · 가이드: `docs/git-workflow.md`.

## 입력

- **type** (필수): `feature` / `fix` / `hotfix` / `refactor` / `chore` / `docs` / `test`
- **topic** (필수): 영어 kebab-case (`member-registration` 등)
- **issue 번호** (선택): 있으면 끝에 `-#123` 부착

브랜치명: `<type>/<topic>` 또는 `<type>/<topic>-#<issue>`

입력이 누락되면 `AskUserQuestion`으로 물을 것.

## 실행 단계

### 1. 사전 검증 (병렬 실행)

- `git status` — uncommitted 변경 확인
- `git branch --show-current` — 현재 브랜치
- `git fetch origin main` — main 최신 동기화

### 2. uncommitted 변경 처리

변경이 있으면 사용자에게 선택을 요청:
- **stash**: `git stash push -m "before-branch:<현재브랜치>"`
- **commit on current branch**: `commit` 스킬에 위임
- **취소**

### 3. 브랜치명 검증

- 프리픽스가 허용 목록에 있는지 확인.
- topic이 영어 kebab-case인지 확인 (대문자·공백·언더스코어 금지).
- 동일명 로컬/원격 브랜치 존재 여부:
  - 로컬: `git rev-parse --verify <name>` (있으면 중단)
  - 원격: `git ls-remote --heads origin <name>` (있으면 중단)

위반·중복 시 사용자에게 수정 요청.

### 4. 브랜치 생성 및 전환

```
git switch -c <type>/<topic> origin/main
```

### 5. 원격 push 및 추적 설정

```
git push -u origin <type>/<topic>
```

push 실패(권한 등) 시 → stash 복구 안내 후 중단.

### 6. 결과 보고

- 생성된 브랜치명
- 원격 추적 상태
- stash가 있다면 복구 명령 (`git stash pop`)
- 다음 단계 안내: 작업 → `commit` 스킬 → `pr` 스킬

## 주의사항

- **항상 origin/main에서 분기.** 다른 브랜치에서 시작하지 않는다.
- 현재 브랜치가 main이 아니어도 무방 (origin/main을 명시적으로 base로 사용하므로).
- 본 스킬은 main에 직접 commit하는 starter-kit 컨텍스트에서는 호출되지 않는다 (메모리 `feedback_main_only_workflow.md`가 차단).
