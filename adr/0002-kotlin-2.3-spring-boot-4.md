# ADR-0002: Kotlin 2.3 + Spring Boot 4.0.6 + Java 25

- 상태: Accepted
- 일시: 2026-05-06
- 갱신: 2026-05-06 — Kotlin 2.2.20 → 2.3.21 (JVM 25 bytecode 지원 위해)

## 맥락

새 starter kit의 언어/프레임워크 베이스 결정.

초기 결정은 Kotlin 2.2.20 (Spring Boot 4의 최소 요구)이었으나, 첫 빌드에서 다음 에러 발생:

```
Inconsistent JVM-target compatibility detected for tasks
'compileJava' (25) and 'compileKotlin' (24).
```

Kotlin 2.2.x의 JvmTarget enum은 JVM_24까지만 정의되어 있어 Java 25 toolchain과 호환 불가.
Kotlin 공식 문서 확인 결과 **Kotlin 2.3.0부터 Java 25 bytecode 생성 지원**.

## 결정

- Java: **25 LTS** (2025-09 출시)
- Spring Boot: **4.0.6** (start.spring.io default GA)
- Kotlin: **2.3.21** (현재 최신 stable, 2026-04-23 출시)

Spring Boot 4의 최소 요구는 2.2.x이지만 2.3.x도 호환. Spring Boot Gradle plugin이 Kotlin BOM 버전을 plugin 버전에 자동 정렬한다.

### 필수 컴파일러 플래그 + JVM target

```kotlin
kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25  // 명시적 설정 필수
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
        )
    }
}
```

`-Xannotation-default-target=param-property`는 Spring Boot 4 공식 권장. Kotlin 2.2부터 변경된 annotation target 기본값 경고 회피.

### 플러그인

- `kotlin("plugin.spring")` — Spring 어노테이션 클래스 자동 open
- `kotlin("plugin.jpa")` — `@Entity` no-arg constructor 자동 생성

## 결과

- cutting-edge 조합이라 일부 서드파티 라이브러리 호환성 검증 필요.
- 특히 `restdocs-api-spec`, `detekt`(K2), `MockK`는 빌드 시 호환성 확인.
- 4.1.0이 임박했으므로 (RC1 출시됨) 1~2달 내 마이그레이션 가능성 인지.
- **JVM 토론 시 항상 jvmTarget을 명시적으로**: `jvmToolchain(N)`만으로는 Kotlin compileKotlin task가 자동 동기화되지 않을 수 있음. compilerOptions에 `jvmTarget = JvmTarget.JVM_N` 명시 권장.

## 거부된 대안

- **Spring Boot 3.5 + Java 21**: 안전하지만 1년 내 다시 마이그레이션 부담.
- **Spring Boot 4.1 RC**: 첫 fork 프로젝트가 RC 위에서 시작 → 위험.
- **Java 24로 다운그레이드**: LTS가 아님. Java 25 LTS 유지가 옳음.
