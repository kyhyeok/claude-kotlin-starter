import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    // detekt: Kotlin 2.3 호환 GA 미출시로 임시 제외. 재도입 가이드: ADR-0007 참고.
    alias(libs.plugins.ktlint)
    alias(libs.plugins.restdocs.api.spec)
    alias(libs.plugins.flyway)
    alias(libs.plugins.jooq.codegen)
}

group = "com.kim"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        // Java 25 bytecode 생성. Kotlin 2.3.0+에서 지원.
        // jvmToolchain만으로는 compileKotlin task가 자동 동기화되지 않으므로 명시적 설정 필수.
        jvmTarget = JvmTarget.JVM_25
        // Spring Boot 4 + Kotlin 2.3 권장 플래그. 자세한 내용: ADR-0002 참고.
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
        )
    }
}

dependencies {
    // 웹/JSON
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    // 영속성 (JPA + jOOQ + Flyway + PostgreSQL)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.jooq)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    // 인증 (JWT - OAuth2 Resource Server + Refresh Token용 Redis)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.nimbus.jose.jwt)

    // 로깅 (JSON 구조 로깅)
    implementation(libs.logstash.logback.encoder)

    // 로컬 개발 시 docker-compose 자동 기동 (PostgreSQL, Redis)
    developmentOnly(libs.spring.boot.docker.compose)

    // 테스트
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.mockito") // MockK 사용
    }
    testImplementation(libs.mockk)
    testImplementation(libs.spring.mockk)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.spring.restdocs.mockmvc)
    testImplementation(libs.restdocs.api.spec.mockmvc)
    testImplementation(libs.spring.security.test)

    // detekt 룰셋: ADR-0007에 따라 임시 제외.
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}

// ============================================================
// 정적 분석 (ktlint만 — detekt은 ADR-0007에 따라 임시 제외)
// ============================================================
ktlint {
    version.set("1.5.0")
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

// ============================================================
// jOOQ codegen (Flyway 적용 후 실행 — Day 2 활성화 예정)
// 시작 시점에는 Flyway 마이그레이션이 없어서 codegen은 비활성화
// 첫 마이그레이션 작성 후 jooq { } 블록으로 활성화하세요.
// ============================================================
// jooq {
//     configurations {
//         create("main") {
//             generationTool {
//                 jdbc {
//                     driver = "org.postgresql.Driver"
//                     url = "jdbc:postgresql://localhost:5432/starter"
//                     user = "starter"
//                     password = "starter"
//                 }
//                 generator {
//                     name = "org.jooq.codegen.KotlinGenerator"
//                     database {
//                         name = "org.jooq.meta.postgres.PostgresDatabase"
//                         inputSchema = "public"
//                     }
//                     target {
//                         packageName = "com.kim.starter.adapter.persistence.jooq"
//                         directory = "build/generated/jooq/main"
//                     }
//                 }
//             }
//         }
//     }
// }

// ============================================================
// REST Docs → OpenAPI 3 → Swagger UI
// 테스트가 통과해야만 문서 생성 → 거짓 문서 불가능
// ============================================================
openapi3 {
    setServer("http://localhost:8080")
    title = "Starter API"
    description = "Spring Kotlin Starter Kit API"
    version = "0.0.1"
    format = "yaml"
}
