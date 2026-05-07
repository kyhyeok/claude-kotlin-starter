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
    // Spring Boot 4부터 Flyway autoconfig가 별도 모듈로 분리됨 (org.springframework.boot.flyway.autoconfigure).
    // starter가 flyway-core + autoconfig를 함께 끌고 옴.
    implementation(libs.spring.boot.starter.flyway)
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

    // jOOQ codegen 전용 클래스패스 — DDLDatabase가 V*.sql을 직접 파싱(ADR-0010).
    jooqCodegen(libs.jooq.meta.extensions)

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
        // glob 패턴은 source set의 baseDir 기준으로 상대화되므로,
        // sourceSets에 srcDir로 등록한 build/generated/jooq/main에는 매칭되지 않는다.
        // 절대 경로 lambda로 명시적으로 제외한다.
        exclude { entry -> entry.file.absolutePath.contains("/build/generated/") }
    }
}

// ============================================================
// jOOQ codegen — Flyway SQL을 DDLDatabase로 직접 파싱한다 (ADR-0010).
// DB/Docker 의존 없이 ./gradlew jooqCodegen 으로 실행 가능.
// 출력: build/generated/jooq/main/com/kim/starter/adapter/persistence/jooq
// ============================================================
jooq {
    configuration {
        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                // V*.sql이 PostgreSQL 문법(BIGSERIAL, TIMESTAMPTZ, now() 등)을 사용 → POSTGRES 파서 강제.
                // 미지정 시 H2 기본 파서가 PostgreSQL 전용 토큰을 거부할 수 있음.
                properties.add(
                    org.jooq.meta.jaxb
                        .Property()
                        .withKey("scripts")
                        .withValue("src/main/resources/db/migration"),
                )
                properties.add(
                    org.jooq.meta.jaxb
                        .Property()
                        .withKey("sort")
                        .withValue("flyway"),
                )
                properties.add(
                    org.jooq.meta.jaxb
                        .Property()
                        .withKey("parser.dialect")
                        .withValue("POSTGRES"),
                )
                // V*.sql이 스키마를 명시하지 않음 → 기본 PUBLIC으로 매핑.
                properties.add(
                    org.jooq.meta.jaxb
                        .Property()
                        .withKey("unqualifiedSchema")
                        .withValue("PUBLIC"),
                )
                // PostgreSQL의 unquoted identifier는 lowercase 정규화.
                properties.add(
                    org.jooq.meta.jaxb
                        .Property()
                        .withKey("defaultNameCase")
                        .withValue("lower"),
                )
                inputSchema = "PUBLIC"
            }
            target {
                packageName = "com.kim.starter.adapter.persistence.jooq"
                directory = "build/generated/jooq/main"
            }
        }
    }
}

// 컴파일/IDE가 생성된 jOOQ 소스를 인식하도록 source set에 등록.
sourceSets["main"].kotlin.srcDir("build/generated/jooq/main")

// 컴파일 전에 codegen이 항상 선행되도록 강제 — 신규 V*.sql 추가 시에도 자동 반영.
tasks.named("compileKotlin") {
    dependsOn("jooqCodegen")
}

// Gradle 9 strict task validation: main source set을 입력으로 잡는 task(ktlint 등)는
// 생성 디렉토리에 대한 의존성을 명시해야 함. ktlint는 ktlint { filter } 블록으로 generated를 검사 제외하지만,
// task input 자체는 여전히 source set과 연결되므로 명시적 mustRunAfter로 순서를 박는다.
tasks.matching { it.name.startsWith("runKtlint") && it.name.endsWith("MainSourceSet") }.configureEach {
    mustRunAfter("jooqCodegen")
    dependsOn("jooqCodegen")
}

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
