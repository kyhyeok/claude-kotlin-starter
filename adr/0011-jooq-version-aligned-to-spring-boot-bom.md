# ADR-0011: jOOQ 버전은 Spring Boot BOM에 정렬한다

- 상태: Accepted
- 일시: 2026-05-07
- 관련: ADR-0010 (DDLDatabase codegen), ADR-0002 (Spring Boot 4 + Kotlin 2.3)

## 맥락

`libs.versions.toml`은 jOOQ 버전을 `3.20.6`(최신 GA)으로 고정해 두었으나, Day 2-2에서 codegen 후 `compileKotlin`이 다음 오류로 실패했다.

```
DefaultCatalog.kt:42:58 Unresolved reference 'VERSION_3_20'.
MembersRecord.kt:61:9   Unresolved reference 'resetTouchedOnNotNull'.
```

`./gradlew dependencies --configuration runtimeClasspath`로 확인:

```
\--- org.jooq:jooq:3.19.32
```

원인:

- Spring Boot 4.0.6의 `spring-boot-dependencies` BOM은 `org.jooq:jooq` 버전을 **3.19.32**로 강제 관리한다.
- jOOQ Gradle 플러그인(`org.jooq.jooq-codegen-gradle:3.20.6`)과 `org.jooq:jooq-meta-extensions:3.20.6`은 **3.20에서 추가된 API**를 사용해 코드를 생성한다 (`Constants.VERSION_3_20`, `Record.resetTouchedOnNotNull` 등).
- 결과: 생성 코드가 런타임 jar(3.19.32)에 존재하지 않는 심볼을 참조 → 컴파일 실패.

Spring Boot 4가 jOOQ를 3.19.x로 박은 이유는 OAuth2 Resource Server / Hibernate 7 / Spring Framework 7과의 검증된 조합이기 때문으로 보인다. starter kit의 보수성과 일치하는 선택.

## 결정

**`libs.versions.toml`의 `jooq` 버전을 Spring Boot BOM이 관리하는 버전(현재 3.19.32)에 정렬한다.**

- `jooq = "3.19.32"` 명시. plugin과 `jooq-meta-extensions`가 동일 버전을 끌고 옴 (`version.ref = "jooq"`).
- BOM 검증 조합을 따름으로써 codegen-runtime API 일관성을 자동 보장.
- 상향 조정 트리거: Spring Boot 차기 BOM이 3.20+를 채택하면 `libs.versions.toml`만 동기화.

## 결과

- codegen이 만든 클래스가 런타임 jar의 API만 사용 → 컴파일 안전.
- jOOQ 버전 결정 권한이 Spring Boot BOM에 위임됨 → starter kit이 추적해야 할 매트릭스가 1개 줄어듦.
- 3.20의 신기능(`resetTouchedOnNotNull` 같은 dirty-tracking 개선, ad-hoc converter API 등)은 일시적으로 사용 불가.

## 거부된 대안

- **dependency-management로 jOOQ 3.20.6 강제**: `dependencyManagement { imports {...} }` 또는 `resolutionStrategy.eachDependency`로 BOM 버전을 override 가능. 하지만 BOM이 검증한 조합을 무시하는 방향은 starter kit의 안전 기준에 어긋남. Spring Boot 측이 다른 컴포넌트(예: Spring Framework 7의 jOOQ binding)와 호환을 보장한 버전을 깨뜨릴 위험.
- **jOOQ 버전을 3.20.6 유지 + plugin은 3.19.32로 분리**: codegen plugin과 plugin이 끌어오는 codegen artifact를 별도로 관리해야 함. 두 곳에서 버전 불일치를 수동 관리 → 잊기 쉬움. 동기 정책(version.ref) 위배.
- **jOOQ Pro/패치 버전 채택**: 라이선스/배포 마찰. starter kit 단계에서 over-engineering.
