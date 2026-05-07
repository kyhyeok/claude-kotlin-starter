# ADR-0007: detekt 임시 제거 (정식 GA 출시까지 대기)

- 상태: Accepted
- 일시: 2026-05-06
- 대체: ADR-0006

## 맥락

ADR-0006에서 Kotlin 2.3.21 호환을 위해 `detekt 2.0.0-alpha.3` 채택을 결정했으나, 빌드 검증 단계에서 두 가지 문제 확인:

1. **alpha의 starter kit 부적합성**: starter kit을 fork하는 모든 신규 프로젝트가 alpha 의존성을 상속받음. 룰/설정 변경, breaking change에 노출됨.
2. **plugin resolve 실패**:
   ```
   Plugin [id: 'dev.detekt', version: '2.0.0-alpha.3'] was not found
   ```
   Gradle Plugin Portal에 alpha 릴리스가 정상 publish되지 않았거나, 사용자가 사용하는 plugin repository에서 접근 불가.

## 결정

starter kit에서 **detekt을 임시 제거**. 정적 분석은 **ktlint만 사용**.

### 보존하는 것

- `detekt.yml` 설정 파일 — 재도입 시 그대로 사용.
- `libs.versions.toml`의 detekt 항목 — 주석 처리로 보존.
- `build.gradle.kts`의 detekt block — 주석 처리.
- `.github/workflows/ci.yml`의 detekt step — 주석 처리.

### 재도입 조건

다음 중 하나를 만족하면 즉시 재도입:

1. detekt 2.0 GA 출시 (현재 alpha.3까지 진행, GA 시점 미정)
2. detekt 1.x에 Kotlin 2.3 호환 패치 release
3. 팀이 정적 분석 룰 가드레일 강화를 요구하고 alpha 위험을 받아들임

### 재도입 절차 (체크리스트)

- [ ] `libs.versions.toml`에서 `detekt = "..."` 주석 해제
- [ ] `libs.versions.toml`의 plugins에서 `detekt = { id = "dev.detekt", ... }` 주석 해제
- [ ] `build.gradle.kts` plugins block에서 `alias(libs.plugins.detekt)` 주석 해제
- [ ] `build.gradle.kts` dependencies에서 `detektPlugins("dev.detekt:detekt-formatting:...")` 주석 해제
- [ ] `build.gradle.kts`에서 `detekt { ... }` 설정 block 주석 해제
- [ ] `.github/workflows/ci.yml`에서 `- name: detekt` step 주석 해제
- [ ] `./gradlew detekt`로 첫 실행 → 룰 위반은 baseline 파일로 흡수
- [ ] 본 ADR을 Superseded로, 새 ADR로 재도입 결정 박제

## 결과

### 수용된 손실

- 복잡도 룰(LongMethod, TooManyFunctions 등) 가드레일 부재.
- 명명 규칙(FunctionNaming 등) 자동 검증 부재.
- 일부는 ktlint로 커버됨 (`no-wildcard-imports`, `filename`, 인덴트, 빈 줄 등).

### ktlint가 커버하는 것

- 코드 스타일 (인덴트, 공백, import 순서)
- 일부 명명 규칙
- `.editorconfig` 기반 룰

ktlint만으로도 PR이 형식적 일관성을 갖도록 보장 가능. 복잡도/명명 등 의미적 룰은 detekt 재도입까지 코드 리뷰가 담당.

## 거부된 대안

- **alpha 사용 강행**: ADR-0006에서 시도했으나 plugin resolve 실패 + starter kit 위험 상속 문제로 거부.
- **Kotlin 2.0.21로 다운그레이드**: ADR-0002의 Java 25 LTS 결정과 충돌. 거부.
- **detekt 영구 제거**: 정적 분석 가드의 가치는 큼. 재도입 가능성 명시.
