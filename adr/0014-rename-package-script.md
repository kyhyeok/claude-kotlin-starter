# 0014. rename-package.sh — base 패키지 일괄 치환 스크립트

- 일자: 2026-05-07
- 상태: Accepted

## 배경

starter kit의 본질은 **fork 후 5분 안에 새 프로젝트로 전환**할 수 있다는 것이다. base 패키지(`com.kim.starter`) 하나를 새 좌표(`com.yourorg.yourapp`)로 바꾸는 일은 IDE refactor로 가능하지만 다음을 빠뜨리기 쉽다.

- ArchUnit 레이어 마킹용 `package-info.java` (Kotlin이 아니므로 IDE의 Kotlin refactor에서 누락 위험)
- jOOQ codegen `target.packageName` (갱신 안 하면 다음 codegen이 옛 패키지에 코드 생성 → 컴파일 깨짐)
- `build.gradle.kts`의 주석 안에 박힌 path 형태(`com/kim/starter/...`)
- `build.gradle.kts`의 `group` 라인 (Maven coordinate)

자동화하지 않으면 매번 새 프로젝트가 같은 함정을 만난다. starter kit은 **자동화를 코드로 박제**하여 이 비용을 0에 수렴시킨다.

## 결정

### 1. argv 형식 — `<old> <new>` 명시

```bash
./scripts/rename-package.sh com.kim.starter com.example.foo [--dry-run]
```

- 명시적 인자가 자동 검출보다 안전. fork된 프로젝트가 이미 한 번 rename된 상태(여러 base 혼재)에서도 동작.
- 단계적 rename(`com.kim.starter` → `com.example.foo` → `com.example.bar`)도 안전하게 반복 가능.

### 2. 치환 범위 — 코드 + 설정만

| 포함 | 제외 |
|---|---|
| `src/**/*.kt` | `README.md` |
| `src/**/*.java` (ArchUnit `package-info.java`) | `CLAUDE.md` |
| `build.gradle.kts` (group + jOOQ target + 주석) | `NEXT_STEPS.md` |
| `settings.gradle.kts` (있을 경우) | `adr/**/*.md` |
| `src/**/application*.yml` (logging 패키지 등) | `개발가이드.md` |

**Why 문서 제외**: ADR과 NEXT_STEPS는 결정 이력. `com.kim.starter` 문자열이 starter kit 출처를 추적하는 단서로 남아야 한다. 사용자가 README의 예시를 자기 패키지로 바꾸고 싶으면 직접 수정. 자동화는 회귀를 막는 곳에만 적용.

### 3. BSD vs GNU sed 분기

```bash
case "$(uname)" in
  Darwin) SED_INPLACE=(sed -i '') ;;
  *)      SED_INPLACE=(sed -i)    ;;
esac
```

macOS BSD `sed -i`는 빈 인자(`''`) 필수. Linux GNU `sed -i`는 받지 않음. 분기하지 않으면 한쪽 OS에서만 작동.

### 4. macOS 기본 bash 3.2 호환

macOS는 GPL 회피로 bash 3.2를 기본 탑재(`brew install bash`로 4.x 가능하지만 starter 사용 전제로 두지 않음). 두 가지 함정:

- `mapfile`/`readarray`는 bash 4.0+. 사용 금지. → `find -exec grep -l {} +` 결과를 변수에 캡처.
- `${VAR//./\/}`가 백슬래시를 보존(`com\/kim\/starter`로 깨짐). → `printf '%s' "$VAR" | tr '.' '/'` 사용.

### 5. group 라인 별도 처리

`group = "com.kim"`처럼 group이 패키지의 prefix인 케이스가 있다(현 starter도 그렇다). 단순 `s|com.kim.starter|...|g` sed로는 안 잡힌다. group 라인을 통째로 `NEW_PKG`로 교체:

```bash
"${SED_INPLACE[@]}" -E "s|^group = \".*\"$|group = \"${NEW_PKG}\"|" build.gradle.kts
```

사용자가 group ≠ 패키지를 원하면 build.gradle.kts에서 직접 수정.

### 6. 디렉토리 이동 — git mv 우선

- git이 있으면 `git mv`로 히스토리 보존, 없으면 `mv` fallback.
- 새 부모 디렉토리(`com/example`)는 `mkdir -p`로 생성, 빈 부모(`com/kim`)는 `find -type d -empty -delete`로 정리.
- 이동 대상이 이미 존재하면 거부(`exit 65`) — 덮어쓰기 사고 방지.

### 7. 안전 가드 + 잔존 검증

- 워크트리 dirty면 거부 (git이 있을 때, `--dry-run`은 예외).
- 변경 후 `grep -rIF "$OLD_PKG"` (fixed string) + `grep -rIF "$OLD_PATH"` 양쪽으로 0건 강제. 1건이라도 남으면 `exit 70`.
- `grep -F` 필수 — regex의 `.` wildcard가 `/`를 매칭해 거짓 음성/양성을 만든다.

## 검증된 함정

| 증상 | 원인 | 해결 |
|---|---|---|
| `mapfile: command not found` | macOS 기본 bash 3.2 (GPL 회피) | mapfile 미사용 — `find -exec ... + ` 결과를 변수에 캡처 |
| dry-run 출력에 `com\/kim\/starter` (백슬래시 escape 보존) | bash 3.2의 `${VAR//./\/}`가 escape를 그대로 둠 | `printf '%s' "$VAR" \| tr '.' '/'` |
| `package-info.java`가 안 잡힘 (잔존 발생) | find pattern이 `*.kt`만 | `\( -name "*.kt" -o -name "*.java" \)` 확장 |
| build.gradle.kts 주석의 path 형태(`com/kim/starter`) 잔존 | sed가 `com.kim.starter` 패턴만 치환 | path 형태 sed 추가 (`s\|$OLD_PATH\|$NEW_PATH\|g`) |
| `group = "com.kim"`이 갱신되지 않음 | group이 OLD_PKG의 prefix이지만 정확히 OLD_PKG는 아니므로 단순 sed 매칭 X | group 라인 정규식 매칭(`^group = ".*"$`)으로 통째 교체 |
| 잔존 검증이 거짓 매칭 | `grep "com.kim.starter"`의 `.`이 `/`를 매칭 → path 형태도 같이 잡거나 거꾸로 놓침 | `grep -F` (fixed string) + path 패턴 별도 검증 |

## 결과 (ROI)

새 프로젝트 시작:

```bash
git clone <starter-repo> my-app && cd my-app
./scripts/rename-package.sh com.kim.starter com.yourorg.myapp
./gradlew clean build         # 컴파일 + 통합 테스트 통과
./gradlew bootRun             # 새 group으로 부팅 (사용자 직접 — ADR-0009)
```

end-to-end 검증(임시 환경): `./gradlew clean build` 33초, 44개 테스트 + ArchUnit + REST Docs + openapi3 모두 통과.

## 거부된 옵션

- **`<new>`만 받고 old 자동 검출**: fork된 starter가 이미 rename되어 여러 base가 섞이면 ambiguous. 명시적 인자가 안전.
- **README/CLAUDE.md까지 치환**: 일관성을 위한 의도였지만, ADR과 NEXT_STEPS.md의 결정 이력에 박힌 `com.kim.starter`가 starter 출처 추적의 단서. 코드/설정만 치환하는 게 정수에 가깝다.
- **bash 4+ 강제 (`mapfile` 사용)**: macOS 사용자에게 `brew install bash` 의존 강제. 진입 장벽이 자동화 ROI를 깎음. POSIX/bash 3.2 호환으로 외부 의존 0.

## 후속 작업

- Day 3-2 (CI 강화) 시 `.github/workflows/ci.yml`에 `./scripts/rename-package.sh com.kim.starter com.example.ci-test --dry-run` smoke 테스트 추가 검토. 스크립트 자체의 회귀를 빌드 사이클이 잡아준다.
