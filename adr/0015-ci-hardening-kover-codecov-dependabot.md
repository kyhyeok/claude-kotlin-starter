# 0015. CI 강화 — concurrency·permissions·Kover·Codecov·Dependabot

- 일자: 2026-05-08
- 상태: Accepted
- 관련: ADR-0007 (detekt 보류), ADR-0011 (BOM 정렬)

## 배경

Day 1~3-1까지 빌드/패키지 자동화가 박혔지만 CI 표면은 최소 골격(`actions/checkout` + `setup-java` + `gradle/actions/setup-gradle` + `./gradlew build`)에 머물러 있었다. starter kit이 새 프로젝트의 기본값을 정의하는 만큼, **fork 직후부터 운영 수준 CI가 작동**해야 한다. 다음 결손을 막아야 한다.

- PR에 연속 push 시 이전 빌드가 누적 → 분당 과금 낭비 + 로그 추적 산만.
- `permissions:` 미명시 → 기본 token이 광범위 권한으로 동작(supply-chain 공격 표면).
- 통합 테스트(Testcontainers) 실패 시 원격 디버깅 수단 부재 — `build/reports/tests/`를 가져올 방법이 없음.
- 커버리지 가시성 0 — 새 코드가 테스트 없이 들어와도 신호가 없음.
- 의존성 갱신이 사람 손에 의존 → Spring Boot/Kotlin 패치 누락이 누적되면 다음 메이저 업그레이드 비용 폭발.

## 결정

### 1. `concurrency.cancel-in-progress: true` + 최소 권한

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read
```

- 같은 ref(브랜치/PR)의 이전 실행은 새 push 시 즉시 취소 → PR 연속 작업의 빌드 큐 누적 차단.
- `permissions: contents: read`로 token 권한 축소. push/comment 등 쓰기 권한은 필요 step에서만 명시적으로 상승.

**거부**: `permissions:` 단위를 step 단위로 잘게 나누는 것 — 현재 build job은 read-only 작업만 하므로 job 단위 선언이 단순.

### 2. Gradle wrapper 검증 활성화

```yaml
- uses: gradle/actions/setup-gradle@v4
  with:
    validate-wrappers: true
```

`gradle/actions/setup-gradle@v4`가 자동으로 wrapper jar의 알려진 checksum을 검증한다. 별도 `gradle/wrapper-validation-action`을 추가하지 않는다 — 기능 중복.

### 3. 실패 시 test report artifact 업로드

```yaml
- name: Upload test reports
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: test-reports
    path:
      - build/reports/tests/
      - build/test-results/
    retention-days: 7
```

Testcontainers 통합 테스트는 CI runner의 docker 데몬 상태에 따라 가끔 flaky → 실패 시 GitHub UI에서 다운로드 가능해야 디버깅 cycle이 짧다. 보존 7일은 retention 비용 ↔ 활용 가능성 균형.

### 4. 커버리지 도구 — Kover (Jacoco 거부)

| 항목 | Kover 0.9.8 | Jacoco |
|---|---|---|
| Kotlin inline 함수 정확도 | ✅ 정확 | ❌ 잘못된 라인 매핑(알려진 한계) |
| Kotlin suspend/default 인자 | ✅ 지원 | ⚠️ 부분 지원 |
| Gradle plugin DSL 통합 | ✅ JetBrains 공식 | ✅ Java 표준 |
| Codecov XML 호환 | ✅ JaCoCo 호환 포맷 | ✅ 원조 포맷 |
| Kotlin 2.3 호환성 | ✅ 검증됨 (이 ADR에서) | ✅ JVM 바이트코드 기반이라 무관 |

→ **Kover 채택**. Kotlin starter가 Kotlin 측정 정확도를 양보할 이유 없음. 통합 비용은 동일.

```kotlin
// libs.versions.toml
kover = "0.9.8"

// build.gradle.kts
kover {
    reports {
        filters {
            excludes {
                packages("com.kim.starter.adapter.persistence.jooq", "com.kim.starter.adapter.persistence.jooq.*")
                classes("com.kim.starter.StarterApplicationKt")
            }
        }
    }
}
```

**제외 대상**:
- `adapter.persistence.jooq.*` — 사람이 작성하지 않는 jOOQ 생성 코드.
- `StarterApplicationKt` — `main` 한 줄. 통합 테스트 컨텍스트 부팅으로 자연 커버되지만 의미 없음.

**거부된 옵션**:
- `kover { reports { verify { rule { minBound(50) } } } }` — coverage threshold를 박아 CI fail 조건으로 만드는 것. starter kit은 도메인 중립이라 의미 있는 임계가 정해지지 않음. 새 프로젝트가 자기 도메인에 맞춰 추가하면 됨.

### 5. Codecov upload — token optional

```yaml
- uses: codecov/codecov-action@v5
  with:
    files: build/reports/kover/report.xml
    token: ${{ secrets.CODECOV_TOKEN }}
    fail_ci_if_error: false
```

- `fail_ci_if_error: false` — Codecov 자체 장애가 CI를 깨뜨리지 않게. coverage는 신호이지 게이트가 아니다.
- `token`은 secrets에서 옵션 주입. public repo는 token 없이도 작동, private repo는 secrets에 박은 후 활성화.

### 6. `koverXmlReport`를 `build`와 함께 실행

```yaml
- run: ./gradlew build koverXmlReport --info
```

- 분리 실행 시 `test` task가 두 번 돌고 Testcontainers도 두 번 띄워야 함 → CI 시간 2배.
- `build koverXmlReport`로 한 번에 실행하면 같은 test execution 결과를 공유.

### 7. Dependabot — gradle + github-actions weekly + group 묶음

```yaml
version: 2
updates:
  - package-ecosystem: gradle
    schedule: { interval: weekly, day: monday, time: "09:00", timezone: Asia/Seoul }
    groups:
      spring-boot: { patterns: ["org.springframework.boot:*", "io.spring.dependency-management"] }
      kotlin: { patterns: ["org.jetbrains.kotlin:*", "org.jetbrains.kotlin.*"] }
      testing: { patterns: ["io.mockk:*", "com.ninja-squad:springmockk", "com.tngtech.archunit:*"] }

  - package-ecosystem: github-actions
    schedule: { interval: weekly, day: monday, time: "09:00", timezone: Asia/Seoul }
```

**그룹 묶음 근거** (ADR-0011 패턴 재사용):
- Spring Boot BOM 하나가 박힌 라이브러리는 BOM PR 한 번으로 일괄 따라가야 한다. 개별 PR로 쪼개면 BOM과 어긋난 버전 PR이 먼저 머지될 수 있어 리스크.
- Kotlin 컴파일러/플러그인은 동기화되어야 한다(`kotlin.jvm` + `kotlin.spring` + `kotlin.jpa` + `kotlin-reflect` 모두 같은 버전). 개별 PR은 의미 없는 churn.
- 테스트 라이브러리(MockK, springmockk, ArchUnit)는 작은 단위라 같은 PR로 묶어도 안전.

**거부**: `daily` 인터벌 — PR 노이즈가 작업 흐름을 깬다. weekly 월요일 09:00 KST가 한국 출근 직후 검토 윈도우와 정렬.

### 8. CodeQL은 보류

CodeQL workflow는 사내 starter(주로 private repo) + GitHub Advanced Security 라이선스 의존이라 도입 비용이 라이선스 결정에 묶임. 새 프로젝트가 GHAS 보유 시 추가하면 됨. NEXT_STEPS의 "선택" 항목으로 남긴다.

## 결과

### 검증

```bash
./gradlew clean build koverXmlReport
# BUILD SUCCESSFUL in 1m 19s
# 25 actionable tasks: 25 executed
# build/reports/kover/report.xml (52KB) 생성 확인
```

- 44개 테스트(단위 + Testcontainers 통합) 통과.
- Kover 0.9.8 + Kotlin 2.3.21 호환성 검증 — Kover가 Kotlin 컴파일러 plugin이 아닌 JaCoCo 에이전트 기반이라 Kotlin 메이저 버전 차이에 robust.
- 알려진 ADR-0013의 openapi3 configuration cache 경고는 그대로(무관).

### 박힌 장점

- PR push마다 분당 비용 누적 차단 (`concurrency.cancel-in-progress`).
- token 권한 축소로 supply-chain 사고 시 영향 범위 최소화 (`permissions: contents: read`).
- 통합 테스트 실패 시 GitHub UI에서 reports 다운로드 가능 — 원격 디버깅 cycle 단축.
- Kotlin inline/suspend의 coverage 정확도 확보 (Kover).
- Codecov 시각화로 PR 단위 coverage delta 확인 가능.
- Spring Boot/Kotlin/Testing 의존성이 group 단위로 자동 갱신 — 누적 부채 차단.

### 영구 박힌 함정

| 증상 | 원인 | 해결 |
|---|---|---|
| `koverXmlReport`를 분리 step으로 실행 시 CI 시간 2배 | test task가 두 번 실행되며 Testcontainers도 두 번 띄움 | 같은 step에서 `./gradlew build koverXmlReport` 단일 실행 |
| jOOQ 생성 코드가 coverage 분모를 부풀려 정확도 왜곡 | `sourceSets.main.kotlin.srcDir("build/generated/jooq/main")` 등록으로 Kover가 자동 포함 | `kover { reports { filters { excludes { packages("...jooq", "...jooq.*") } } } }` |
| Codecov 장애가 CI fail | `codecov-action`의 기본 `fail_ci_if_error` 동작 | `fail_ci_if_error: false` (coverage는 게이트가 아닌 신호) |
| Dependabot이 BOM과 어긋난 개별 PR을 먼저 만들어 머지 시 빌드 깨짐 | 개별 라이브러리 PR이 BOM PR보다 먼저 도착 가능 | `groups: spring-boot: { patterns: ["org.springframework.boot:*", ...] }`로 BOM과 함께 묶음 |
| Dependabot daily PR 폭주 | 기본 `daily` interval | weekly + open-pull-requests-limit: 5 |

## 후속

- **Day 3-3 (Micrometer/Prometheus)**: coverage가 노출되었으므로 다음은 메트릭 가시성. CI 변경은 추가 없음.
- **Day 3-4 (detekt 2.0 GA)**: detekt 도입 시 ci.yml의 `# - name: detekt` 주석을 활성화하고 Kover와 같은 step에 합류.
- **threshold 도입 검토**: 새 프로젝트가 fork 후 도메인이 정해지면 `kover { reports { verify { rule { minBound(N) } } } }` 추가 권장. starter는 중립 유지.
- **CodeQL**: GHAS 라이선스 보유 시 `.github/workflows/codeql.yml` 추가. NEXT_STEPS "선택" 유지.
