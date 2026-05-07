# ADR-0008: GitHub Flow + squash-merge

- 상태: Accepted
- 일시: 2026-05-07

## 맥락

본 starter-kit은 자체적으로는 단일 작업자가 main에 직접 commit하는 방식으로 운영된다. 그러나 이 starter에서 파생되는 모든 신규 프로젝트는 다인 협업 환경을 전제한다.

신규 프로젝트마다 git 워크플로우를 처음부터 정의하면 다음 비용이 발생:

1. 워크플로우 결정의 비일관성 — 프로젝트마다 GitFlow / Trunk-based / GitHub Flow 혼재.
2. PR 템플릿·브랜치 네이밍·머지 전략을 매번 재정의.
3. LLM 보조 자산(스킬·커맨드)도 매번 재구축.

starter 단계에서 워크플로우를 박제하면 위 비용을 한 번만 지불한다.

## 결정

**GitHub Flow + squash-merge**를 starter의 표준 워크플로우로 채택. 파생 프로젝트는 별도 결정 없이 이 워크플로우를 상속받는다.

### 핵심 규칙

- **브랜치**: `main`만 장기 브랜치. 모든 작업은 `<prefix>/<topic>` 단기 브랜치에서.
  허용 브랜치 프리픽스: `feature` / `fix` / `hotfix` / `refactor` / `chore` / `docs` / `test`.
- **커밋 type** (Conventional Commits 1.0.0): `feat` · `fix` · `docs` · `style` · `refactor` · `perf` · `test` · `build` · `ci` · `chore` · `revert`. Breaking change는 `type!:`.
  > 브랜치 프리픽스와 커밋 type은 별도 체계. `hotfix/` 브랜치의 커밋 type은 `fix`.
- **머지**: PR 단위 squash-merge. main의 commit 1개 = PR 1개. 머지 후 작업 브랜치 삭제.
- **rebase 정책**: PR 내부 commit 정리용 rebase는 허용. main에 push된 commit의 rebase는 금지.
- **CI**: PR 생성 시 `./gradlew clean build` 자동 실행. 실패 시 머지 차단.
- **이슈 연동**: PR body에 `Closes #N` 형태로 issue 링크.

### 본 starter-kit의 예외

이 starter-kit 자체는 단일 작업자 운영이므로 main에 직접 commit한다. 사용자 메모리(`feedback_main_only_workflow.md`)가 starter 작업 컨텍스트의 LLM 행동을 제약한다. 이 예외는 starter 작업 컨텍스트에서만 유효하며, 메모리는 git 추적 외부이므로 파생 프로젝트로 propagate되지 않는다.

## 거부된 대안

- **GitFlow** (`develop` + `release/*` + `hotfix/*`): 장기 브랜치 다수, 머지 충돌 비용 높음. 릴리즈 사이클이 짧고 CD 지향인 본 스택과 부적합.
- **Trunk-based development** (PR 없이 main 직접 push, feature flag 기반): 코드 리뷰 강제력 약화. 사내 리뷰 문화 확립 전 단계로 시기상조.
- **PR 머지 시 merge commit 유지**: 히스토리에 노이즈 commit이 누적. 한 PR = 한 commit이 더 명료.
- **rebase merge**: main의 hash가 변경되어 추적이 어려움. squash가 단순.

## 결과

### 자동화 자산 (본 ADR과 함께 도입)

- `.claude/skills/commit/SKILL.md` — 커밋 (기존)
- `.claude/skills/branch/SKILL.md` — 브랜치 생성·전환 (신규)
- `.claude/skills/pr/SKILL.md` — PR 생성 (신규)
- `docs/git-workflow.md` — 사람 대상 가이드 (신규)

### 폐기 자산

- `.claude/git/{branch,merge,pr,commit}.md` — 표준 위치가 아니고, 가상 옵션·UX가 다수. 핵심 개념은 위 자산에 흡수.

### 파생 프로젝트가 별도 수행할 GitHub repo 설정

코드로 박제 불가하므로 본 ADR이 명시 근거:

- main 브랜치 보호: 직접 push 금지, PR + CI green 필수.
- squash-merge 외 옵션 비활성화 (merge commit / rebase merge 금지).
- 머지 후 작업 브랜치 자동 삭제.
- Required status checks: `./gradlew clean build`.

### 재검토 트리거

다음 중 하나가 발생하면 본 ADR 재검토:

1. 팀 규모가 10인 이상이 되어 release train이 필요해질 때 (GitFlow 검토).
2. CD 빈도가 일 10회 이상이 되어 PR-per-change가 병목이 될 때 (Trunk-based + feature flag 검토).
3. main 히스토리가 squash로 인해 추적성이 떨어진다는 회고 결과가 나올 때 (merge commit 옵션 재검토).
