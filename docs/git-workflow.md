# Git 워크플로우 가이드

본 문서는 이 starter-kit에서 파생된 프로젝트의 표준 git 워크플로우를 다룬다.

- **근거**: `adr/0008-github-flow-squash-merge.md`
- **자동화 스킬**: `commit` · `branch` · `pr`
- **starter 자체는 main-only**. 본 가이드는 파생 프로젝트 대상.

---

## 1. 워크플로우 한눈에

```
main ────●────●────●────●────●  (보호 브랜치, squash 커밋만)
          \  /  \  /  \  /
           PR    PR    PR
           │     │     │
    feature/x  fix/y  chore/z   (단기 브랜치, 머지 후 삭제)
```

- **장기 브랜치**: `main` 1개.
- **단기 브랜치**: 작업 단위. 머지 후 즉시 삭제.
- **머지**: 항상 PR + squash-merge.

---

## 2. 브랜치 네이밍

### 프리픽스

| 프리픽스 | 용도 |
|---|---|
| `feature/` | 새 기능 |
| `fix/` | 버그 수정 |
| `hotfix/` | 긴급 운영 수정 |
| `refactor/` | 동작 변경 없는 구조 개선 |
| `chore/` | 빌드·설정·의존성 |
| `docs/` | 문서만 변경 |
| `test/` | 테스트만 추가/수정 |

### 패턴

```
✅ feature/member-registration
✅ fix/jwt-expiry-validation
✅ chore/upgrade-spring-boot-4.0.1

❌ feat-foo            # 슬래시 없음
❌ FEATURE/Foo         # 대문자
❌ feature/foo bar     # 공백
❌ tmp                 # 의도 불명
```

브랜치명은 영어 kebab-case. issue 번호가 있으면 끝에 `-#123`.

---

## 3. 커밋 메시지

`commit` 스킬에 위임. 형식:

```
type: 한국어 제목 (50자 이내)

본문 (선택)
```

### type 목록 (Conventional Commits 1.0.0)

`feat` · `fix` · `docs` · `style` · `refactor` · `perf` · `test` · `build` · `ci` · `chore` · `revert`

> `hotfix/`는 **브랜치 프리픽스** 전용. 커밋 type은 `fix`로 통일 (Conventional Commits 명세에 `hotfix` type 없음).

### Breaking change

API·DB 스키마 등 호환성 깨는 변경은 type 뒤에 `!`:
```
feat!: 회원 ID 타입을 Long에서 UUID로 변경
```
또는 본문에 `BREAKING CHANGE: <설명>` 푸터.

자세한 절차: `.claude/skills/commit/SKILL.md`.

PR이 squash-merge되므로 작업 브랜치 내부의 중간 커밋 메시지는 정리 부담이 낮다. **최종 squash 커밋의 제목/본문이 main 히스토리에 박힌다** — PR 제목과 본문이 그대로 squash 커밋이 되므로 PR 메타데이터에 신경 쓸 것.

---

## 4. PR 절차

### 생성

`pr` 스킬에 위임. 자세한 절차: `.claude/skills/pr/SKILL.md`.

### 제목

브랜치 프리픽스와 conventional commit type을 일관되게 매핑:

| 브랜치 프리픽스 | PR 제목 prefix |
|---|---|
| `feature/` | `feat:` |
| `fix/` `hotfix/` | `fix:` |
| `refactor/` | `refactor:` |
| `chore/` | `chore:` |
| `docs/` | `docs:` |
| `test/` | `test:` |

예: `feature/member-registration` → `feat: 회원 가입 API 추가`

### 본문 템플릿

```markdown
## 변경사항
- [핵심 변경 1]
- [핵심 변경 2]

## 테스트
- [ ] 단위 테스트 추가/통과
- [ ] 통합 테스트 추가/통과
- [ ] 수동 검증: ...

## 관련 이슈
Closes #123
```

### 사이즈 가이드

- **권장**: 변경 라인 < 400, 파일 < 20개.
- 초과 시 PR 분리를 우선 고려. squash-merge 환경에서 큰 PR은 revert 비용이 높다.

### 리뷰

- 최소 1명 승인 + CI green 후 머지.
- 리뷰어 지정은 CODEOWNERS 또는 수동.

---

## 5. 머지 전략

### squash-merge 기본

- main의 commit 1개 = PR 1개. PR 제목이 main의 commit 제목.
- 작업 브랜치의 중간 커밋은 main 히스토리에서 사라짐.

### main 동기화

작업 브랜치를 main에 정렬:

```bash
git fetch origin
git rebase origin/main
```

> rebase는 push되지 않은 commit에만 안전하다. 다른 사람이 같은 브랜치에서 작업 중이면 `--force-with-lease` 필수.

### 충돌 해결

```bash
git status                     # 충돌 파일 확인
# 파일 편집 후
git add <files>
git rebase --continue
# 포기하려면
git rebase --abort
```

---

## 6. 브랜치 보호 (GitHub repo 설정)

repo Settings → Branches → main 규칙:

- [ ] Require a pull request before merging
- [ ] Require approvals (≥1)
- [ ] Require status checks to pass — `./gradlew clean build`
- [ ] Require branches to be up to date before merging
- [ ] Allow squash merging만 활성화. merge commit / rebase merge 비활성화
- [ ] Automatically delete head branches after merge

---

## 7. 자주 쓰는 명령

```bash
# 새 브랜치
git switch -c feature/<topic> origin/main
git push -u origin feature/<topic>

# main 동기화
git fetch origin && git rebase origin/main

# PR 생성
gh pr create --fill

# PR 상태/체크
gh pr status
gh pr checks

# 머지된 로컬 브랜치 정리
git branch --merged main | grep -vE '^\*|main$' | xargs -n1 git branch -d
git fetch --prune
```
