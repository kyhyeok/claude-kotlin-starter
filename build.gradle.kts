import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    // detekt: Kotlin 2.3 호환 GA 미출시로 임시 제외. 재도입 가이드: ADR-0007 참고.
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
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
    // Prometheus 노출 — /actuator/prometheus 엔드포인트. Spring Boot BOM이 버전 관리 (ADR-0016).
    implementation(libs.micrometer.registry.prometheus)
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

    // Swagger UI (REST Docs로 만든 OpenAPI spec을 정적 호스팅).
    // webjars-locator-core가 있으면 webjar 자원에 버전 없이 접근 가능 → 업그레이드 시 HTML 수정 불필요.
    implementation(libs.swagger.ui)
    implementation(libs.webjars.locator.core)

    // 로컬 개발 시 docker-compose 자동 기동 (PostgreSQL, Redis)
    developmentOnly(libs.spring.boot.docker.compose)

    // 테스트
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.mockito") // MockK 사용
    }
    testImplementation(libs.mockk)
    testImplementation(libs.spring.mockk)
    // Spring Boot 4.0의 분리된 테스트 자동구성 모듈 (libs.versions.toml 주석 참고).
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.spring.boot.restdocs)
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
    // REST Docs snippet 디렉토리를 test의 output으로 등록 → Gradle build cache가 snippet을 함께
    // 캐시·복원하므로 cache hit 시에도 openapi3가 빈 spec을 만들지 않는다.
    outputs.dir(layout.buildDirectory.dir("generated-snippets"))
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
// 커버리지 — Kover (ADR-0015)
// 모든 Test task의 coverage를 자동 수집 → build/reports/kover/report.xml (JaCoCo 호환).
// Codecov가 이 XML을 그대로 업로드한다.
// ============================================================
kover {
    reports {
        filters {
            excludes {
                // jOOQ 생성 코드는 coverage 대상 아님 — 사람이 작성하지 않는 코드.
                packages("com.kim.starter.adapter.persistence.jooq", "com.kim.starter.adapter.persistence.jooq.*")
                // Spring Boot 진입점 — main 메서드 한 줄짜리, 통합 테스트가 컨텍스트 부팅으로 자연 커버.
                classes("com.kim.starter.StarterApplicationKt")
            }
        }
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
// 테스트가 통과해야만 문서 생성 → 거짓 문서 불가능 (snippet은 test task가 생성).
// ============================================================
openapi3 {
    setServer("http://localhost:8080")
    title = "Starter API"
    description = "Spring Kotlin Starter Kit API"
    version = "0.0.1"
    format = "yaml"
}

// restdocs-api-spec 0.20.x의 OpenApi3Task는 Jackson StdDateFormat을 직접 들고 다녀
// Gradle 9의 configuration cache 직렬화에서 java.text.DateFormat의 strong encapsulation에 막힘.
// task 단위 opt-out으로 해결. plugin이 호환되면 제거 (issue: ePages-de/restdocs-api-spec).
// openapi3 task는 plugin이 lazy하게 등록 → `matching`으로 등록 시점에 configure.
tasks
    .matching { it.name == "openapi3" }
    .configureEach {
        notCompatibleWithConfigurationCache(
            "restdocs-api-spec 0.20.1: OpenApi3Task의 ObjectMapper(StdDateFormat)가 configuration cache 직렬화 미호환",
        )
        // snippets는 test가 생성하므로 의존성 명시.
        dependsOn("test")
    }

// `./gradlew build` 한 번으로 OpenAPI spec까지 함께 갱신되도록 통합. 거짓 문서 방지의 핵심.
// build → assemble + check 의 confining check chain이 아닌 build에 직접 hook하여 순환 회피.
tasks.named("build") {
    dependsOn("openapi3")
}

// Swagger UI가 호스팅할 spec은 OpenApiSpecController가 런타임에 노출한다.
// 두 환경 분리:
//   1. bootJar: copyOpenApiSpec → bootJar의 from()으로 BOOT-INF/classes/static/api-spec에 박음.
//      sourceSet output에는 등록하지 않는다 — main classes에 결합하면
//      `test → main classes → copyOpenApiSpec → openapi3 → test` 형태의 circular가 발생하기 때문.
//   2. bootRun: spec은 jar에 없으나 controller가 build/api-spec/openapi3.yaml(filesystem)을 fallback.
//      `./gradlew build` 한 번 돌리면 spec이 생성되어 그 이후 bootRun에서 보임.
val copyOpenApiSpec by tasks.registering(Copy::class) {
    dependsOn("openapi3")
    from(layout.buildDirectory.dir("api-spec"))
    into(layout.buildDirectory.dir("swagger-static/static/api-spec"))
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    dependsOn(copyOpenApiSpec)
    from(layout.buildDirectory.dir("swagger-static")) {
        into("BOOT-INF/classes")
    }
}
