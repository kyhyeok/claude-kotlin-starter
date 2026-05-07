---
name: pr
description: 현재 브랜치를 main 대상으로 PR을 생성합니다. 브랜치명·커밋 히스토리에서 제목/본문을 자동 구성합니다.
allowed-tools: Bash, AskUserQuestion
---

# Pull Request 생성 스킬

워크플로우 근거: `adr/0008-github-flow-squash-merge.md` · 가이드: `docs/git-workflow.md`.

## 전제

- `gh` CLI 설치 및 인증 완료 (`gh auth status`로 확인 가능).
- 현재 브랜치가 main이 아님.
- 적어도 1개의 커밋이 origin/main과 다름.

## 실행 단계

### 1. 사전 검증 (병렬 실행)

- `gh auth status`
- `git branch --show-current`
- `git status`
- `git fetch origin main`
- `git log origin/main..HEAD --oneline` — PR에 들어갈 커밋
- `git diff --stat origin/main..HEAD` — 변경 규모

### 2. 검증 실패 시 처리

- **현재 브랜치 = main** → 중단. `branch` 스킬로 작업 브랜치를 만들라고 안내.
- **uncommitted 변경 존재** → 사용자에게 commit / stash / 취소 선택.
- **main과 차이 없음** → 중단. 커밋이 필요함을 안내.
- **시크릿 의심 파일 변경** (`.env`, `*.key`, `*credentials*`) → 중단하고 보고.
- **사이즈 초과** (변경 > 400줄 또는 파일 > 20개) → 사용자에게 분리 권유 후 진행 여부 확인.

### 3. 원격 동기화

- 로컬 브랜치가 origin에 없으면 `git push -u origin <branch>`.
- 로컬이 origin보다 앞서면 `git push`.
- 로컬이 origin보다 뒤지면 사용자 확인 후 `git pull --rebase origin <branch>`.

### 4. PR 메타데이터 결정

#### 제목

브랜치명 프리픽스 → conventional commit type 매핑:

| 브랜치 프리픽스 | PR 제목 prefix |
|---|---|
| `feature/` | `feat:` |
| `fix/` `hotfix/` | `fix:` |
| `refactor/` | `refactor:` |
| `chore/` | `chore:` |
| `docs/` | `docs:` |
| `test/` | `test:` |

토픽은 한국어로 의역. **사용자 확인 필수.**

#### 본문 템플릿

```markdown
## 변경사항
- {커밋 메시지에서 추출한 항목 1}
- {커밋 메시지에서 추출한 항목 2}

## 테스트
- [ ] 단위 테스트 추가/통과
- [ ] 통합 테스트 추가/통과
- [ ] 수동 검증: {기록 필요}

## 관련 이슈
{브랜치명에 -#N이 있으면 "Closes #N", 없으면 "없음"}
```

#### draft 여부

사용자에게 `--draft`로 만들지 확인.

### 5. PR 생성

```
gh pr create \
  --base main \
  --head <current-branch> \
  --title "<title>" \
  --body "$(cat <<'EOF'
<body>
EOF
)" \
  [--draft]
```

`<body>`는 HEREDOC으로 전달. 따옴표·백틱 이스케이프 주의.

### 6. 결과 보고

- PR URL (gh가 출력)
- CI 상태 확인 명령: `gh pr checks`
- 다음 단계: 리뷰어 지정, CI green 대기

## 주의사항

- **CI 통과 전 머지 금지.** 본 스킬은 PR 생성까지만 담당.
- **머지는 GitHub UI 또는 `gh pr merge --squash`로.** 로컬 merge 금지 (squash-merge 정책 준수).
- **PR 제목/본문이 squash 커밋이 되어 main에 박힌다** — 메타데이터를 신중히 작성.
- 본 스킬은 main에 직접 commit하는 starter-kit 컨텍스트에서는 호출되지 않는다.
