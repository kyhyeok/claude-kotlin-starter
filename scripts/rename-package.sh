#!/usr/bin/env bash
# starter kit의 base 패키지를 일괄 치환한다.
# 자세한 결정 근거: adr/0014-rename-package-script.md
#
# 사용법:
#   ./scripts/rename-package.sh <old-package> <new-package> [--dry-run]
#
# 예시:
#   ./scripts/rename-package.sh com.kim.starter com.example.foo --dry-run
#   ./scripts/rename-package.sh com.kim.starter com.example.foo

set -euo pipefail

usage() {
  cat >&2 <<EOF
사용법: $0 <old-package> <new-package> [--dry-run]

인자:
  <old-package>   현재 base 패키지 (예: com.kim.starter)
  <new-package>   변경할 base 패키지 (예: com.example.foo)
  --dry-run       실제 변경 없이 대상 파일만 출력 (선택)

종료 코드:
  0   성공
  64  잘못된 인자
  65  실행 가능한 상태 아님 (워크트리 dirty 등)
EOF
}

if [[ $# -lt 2 || $# -gt 3 ]]; then
  usage
  exit 64
fi

OLD_PKG="$1"
NEW_PKG="$2"
DRY_RUN="${3:-}"

# 패키지명 검증 (Java 식별자 규칙: 소문자.숫자.언더스코어, 점으로 구분)
PKG_REGEX='^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$'
if ! [[ "$OLD_PKG" =~ $PKG_REGEX ]]; then
  echo "ERROR: <old-package>가 유효한 Java 패키지 형식이 아닙니다: $OLD_PKG" >&2
  exit 64
fi
if ! [[ "$NEW_PKG" =~ $PKG_REGEX ]]; then
  echo "ERROR: <new-package>가 유효한 Java 패키지 형식이 아닙니다: $NEW_PKG" >&2
  exit 64
fi
if [[ "$OLD_PKG" == "$NEW_PKG" ]]; then
  echo "ERROR: <old>와 <new>가 동일합니다." >&2
  exit 64
fi
if [[ -n "$DRY_RUN" && "$DRY_RUN" != "--dry-run" ]]; then
  echo "ERROR: 알 수 없는 옵션: $DRY_RUN" >&2
  usage
  exit 64
fi

# 프로젝트 루트 진입 (스크립트는 scripts/에 위치)
cd "$(dirname "$0")/.."
PROJECT_ROOT="$(pwd)"

# 워크트리 cleanliness — 실제 실행 시에만 강제
if [[ "$DRY_RUN" != "--dry-run" ]]; then
  if [[ -d .git ]]; then
    if ! git diff --quiet || ! git diff --cached --quiet; then
      echo "ERROR: 워크트리가 dirty 합니다. 커밋/스태시 후 다시 실행하세요." >&2
      git status -s >&2
      exit 65
    fi
  else
    echo "WARN: git 저장소가 아닙니다. 변경을 되돌리려면 백업 필수." >&2
  fi
fi

# macOS 기본 bash 3.2는 ${VAR//./\/}에서 백슬래시를 보존(com\/kim\/starter)하므로 tr 사용
OLD_PATH="$(printf '%s' "$OLD_PKG" | tr '.' '/')"
NEW_PATH="$(printf '%s' "$NEW_PKG" | tr '.' '/')"

echo "이전 패키지: $OLD_PKG  → ($OLD_PATH)"
echo "새 패키지:   $NEW_PKG  → ($NEW_PATH)"
echo "프로젝트 루트: $PROJECT_ROOT"
[[ "$DRY_RUN" == "--dry-run" ]] && echo "모드: DRY-RUN (변경 없음)"
echo

# 변경 대상을 라벨과 함께 출력. mapfile은 bash 4.0+ 한정이므로 -exec 패턴 사용.
print_section() {
  local label="$1"
  local hits="$2"
  echo "=== $label ==="
  if [[ -z "$hits" ]]; then
    echo "  (없음)"
  else
    printf '%s\n' "$hits" | sed 's|^|  |'
  fi
  echo
}

# 변경 대상 — 코드 + 설정만 (README/ADR/CLAUDE.md/NEXT_STEPS.md는 history 보존)
# *.java도 포함 — ArchUnit 레이어 마킹용 package-info.java가 있으므로
KT_HITS="$(find src -type f \( -name "*.kt" -o -name "*.java" \) -not -path "*/build/*" \
  -exec grep -l "$OLD_PKG" {} + 2>/dev/null || true)"
print_section "Kotlin/Java 소스 (*.kt / *.java, build/ 제외)" "$KT_HITS"

BUILD_HITS=""
for f in build.gradle.kts settings.gradle.kts; do
  if [[ -f "$f" ]] && grep -q "$OLD_PKG" "$f"; then
    BUILD_HITS+="${f}"$'\n'
  fi
done
print_section "빌드 스크립트" "${BUILD_HITS%$'\n'}"

YML_HITS="$(find src -type f \( -name "application*.yml" -o -name "application*.yaml" \) \
  -exec grep -l "$OLD_PKG" {} + 2>/dev/null || true)"
print_section "resources (application*.yml)" "$YML_HITS"

DIRS="$(find src -type d -path "*/$OLD_PATH" -not -path "*/build/*" 2>/dev/null || true)"
print_section "디렉토리 이동 대상" "$DIRS"

if [[ "$DRY_RUN" == "--dry-run" ]]; then
  echo "DRY-RUN 완료. 실제 치환은 --dry-run 없이 다시 실행하세요."
  exit 0
fi

# ============================================================
# 실제 치환 단계
# ============================================================

# BSD sed(macOS)는 -i 뒤에 빈 인자 필수, GNU sed(Linux)는 받지 않음.
case "$(uname)" in
  Darwin) SED_INPLACE=(sed -i '') ;;
  *)      SED_INPLACE=(sed -i)    ;;
esac

# 패키지명의 점(.)은 sed 정규식 메타문자이므로 escape.
# bash 3.2의 ${VAR//./\\.}는 백슬래시를 보존해 \\. 두 개가 들어가므로 printf|sed 사용.
OLD_PKG_REGEX="$(printf '%s' "$OLD_PKG" | sed 's/\./\\./g')"

echo "=== Kotlin/Java 소스 치환 (package / import / 코드 내 문자열) ==="
KT_TARGETS="$(find src -type f \( -name "*.kt" -o -name "*.java" \) -not -path "*/build/*" \
  -exec grep -l "$OLD_PKG" {} + 2>/dev/null || true)"
if [[ -z "$KT_TARGETS" ]]; then
  echo "  (변경 대상 없음)"
else
  echo "$KT_TARGETS" | while IFS= read -r f; do
    "${SED_INPLACE[@]}" "s|${OLD_PKG_REGEX}|${NEW_PKG}|g" "$f"
    echo "  치환: $f"
  done
fi
echo

# 검증 — *.kt / *.java 안에 OLD_PKG 잔존이 없어야 함
REMAINING="$(find src -type f \( -name "*.kt" -o -name "*.java" \) -not -path "*/build/*" \
  -exec grep -l "$OLD_PKG" {} + 2>/dev/null || true)"
if [[ -n "$REMAINING" ]]; then
  echo "ERROR: 치환 후에도 OLD_PKG 잔존:" >&2
  echo "$REMAINING" >&2
  exit 70
fi
echo "검증: *.kt / *.java 안의 ${OLD_PKG} 참조 0건"
echo

# ============================================================
# 디렉토리 이동 (히스토리 보존을 위해 git이 있으면 git mv)
# ============================================================

USE_GIT="false"
if [[ -d .git ]] && command -v git >/dev/null 2>&1; then
  USE_GIT="true"
fi

move_dir() {
  local old="$1"
  local new="$2"
  if [[ ! -d "$old" ]]; then
    echo "  스킵: $old (없음)"
    return 0
  fi
  if [[ -e "$new" ]]; then
    echo "ERROR: 이동 대상이 이미 존재: $new" >&2
    exit 65
  fi
  mkdir -p "$(dirname "$new")"
  if [[ "$USE_GIT" == "true" ]]; then
    git mv "$old" "$new"
  else
    mv "$old" "$new"
  fi
  echo "  이동: $old → $new"
}

echo "=== 디렉토리 이동 ==="
for sourceset in main test; do
  move_dir "src/$sourceset/kotlin/$OLD_PATH" "src/$sourceset/kotlin/$NEW_PATH"
done

# 비게 된 중간 디렉토리(예: com/kim) 정리
find src -type d -empty -delete 2>/dev/null || true
echo

# ============================================================
# build.gradle.kts: group + jOOQ target packageName + 주석
# ============================================================

echo "=== build.gradle.kts 갱신 ==="
if [[ -f build.gradle.kts ]]; then
  # group 라인은 OLD_PKG의 prefix일 수 있으므로 별도 치환 (예: group="com.kim" 인데 패키지는 com.kim.starter)
  "${SED_INPLACE[@]}" -E "s|^group = \".*\"$|group = \"${NEW_PKG}\"|" build.gradle.kts
  # 패키지 형태(jOOQ target.packageName)
  "${SED_INPLACE[@]}" "s|${OLD_PKG_REGEX}|${NEW_PKG}|g" build.gradle.kts
  # path 형태(jOOQ output path 주석 등)
  "${SED_INPLACE[@]}" "s|${OLD_PATH}|${NEW_PATH}|g" build.gradle.kts
  echo "  치환: build.gradle.kts (group=\"${NEW_PKG}\", ${OLD_PKG}/${OLD_PATH} → ${NEW_PKG}/${NEW_PATH})"
fi
echo

# ============================================================
# settings.gradle.kts + application*.yml — 있는 경우만
# ============================================================

if [[ -f settings.gradle.kts ]] && grep -q "$OLD_PKG" settings.gradle.kts; then
  "${SED_INPLACE[@]}" "s|${OLD_PKG_REGEX}|${NEW_PKG}|g" settings.gradle.kts
  echo "  치환: settings.gradle.kts"
fi

YML_AFFECTED="$(find src -type f \( -name "application*.yml" -o -name "application*.yaml" \) \
  -exec grep -l "$OLD_PKG" {} + 2>/dev/null || true)"
if [[ -n "$YML_AFFECTED" ]]; then
  echo "$YML_AFFECTED" | while IFS= read -r f; do
    "${SED_INPLACE[@]}" "s|${OLD_PKG_REGEX}|${NEW_PKG}|g" "$f"
    echo "  치환: $f"
  done
fi

# ============================================================
# 최종 검증
# ============================================================

echo
echo "=== 최종 잔존 검증 ==="
# -F: fixed string (regex meta 무시), -I: binary 제외
SCAN_TARGETS=("src" "build.gradle.kts")
[[ -f settings.gradle.kts ]] && SCAN_TARGETS+=("settings.gradle.kts")
PKG_REMAIN="$(grep -rIF "$OLD_PKG"  "${SCAN_TARGETS[@]}" 2>/dev/null || true)"
PATH_REMAIN="$(grep -rIF "$OLD_PATH" "${SCAN_TARGETS[@]}" 2>/dev/null || true)"
if [[ -n "$PKG_REMAIN" || -n "$PATH_REMAIN" ]]; then
  echo "ERROR: 잔존 참조:" >&2
  [[ -n "$PKG_REMAIN" ]]  && { echo "[$OLD_PKG]"  >&2; echo "$PKG_REMAIN"  >&2; }
  [[ -n "$PATH_REMAIN" ]] && { echo "[$OLD_PATH]" >&2; echo "$PATH_REMAIN" >&2; }
  exit 70
fi
echo "  ${OLD_PKG} 및 ${OLD_PATH} 참조 0건 (코드 + 빌드 스크립트 + resources)"
echo
echo "다음 단계:"
echo "  1. ./gradlew clean build         # 컴파일/테스트 검증"
echo "  2. ./gradlew bootRun             # 새 group으로 부팅 (사용자 직접 — ADR-0009)"
echo "  3. git status / git diff         # 변경 검토 후 commit"
