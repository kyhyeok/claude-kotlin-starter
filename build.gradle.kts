import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
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
        // jvmToolchain만으로는 compileKotlin이 자동 동기화되지 않음 (ADR-0002).
        jvmTarget = JvmTarget.JVM_25
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
        )
    }
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.jooq)
    // Spring Boot 4부터 Flyway autoconfig가 별도 모듈로 분리 → starter 사용 필수.
    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.nimbus.jose.jwt)

    implementation(libs.logstash.logback.encoder)

    implementation(libs.swagger.ui)
    implementation(libs.webjars.locator.core)

    developmentOnly(libs.spring.boot.docker.compose)

    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.mockito")
    }
    testImplementation(libs.mockk)
    testImplementation(libs.spring.mockk)
    // Spring Boot 4.0에서 분리된 테스트 자동구성 모듈 (ADR-0013).
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.spring.boot.restdocs)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.spring.restdocs.mockmvc)
    testImplementation(libs.restdocs.api.spec.mockmvc)
    testImplementation(libs.spring.security.test)

    jooqCodegen(libs.jooq.meta.extensions)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
    // snippet 디렉토리를 test output으로 등록 → cache hit 시에도 openapi3가 빈 spec 안 만듦.
    outputs.dir(layout.buildDirectory.dir("generated-snippets"))
}

ktlint {
    version.set("1.5.0")
    filter {
        // glob은 source set baseDir 기준 상대화 → srcDir로 추가된 build/generated/jooq에 미매칭.
        exclude { entry -> entry.file.absolutePath.contains("/build/generated/") }
    }
}

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

// jOOQ codegen은 Flyway SQL을 DDLDatabase로 직접 파싱 (ADR-0010).
jooq {
    configuration {
        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
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
                // V*.sql의 PostgreSQL 전용 토큰을 H2 기본 파서가 거부 → POSTGRES 강제.
                properties.add(
                    org.jooq.meta.jaxb
                        .Property()
                        .withKey("parser.dialect")
                        .withValue("POSTGRES"),
                )
                properties.add(
                    org.jooq.meta.jaxb
                        .Property()
                        .withKey("unqualifiedSchema")
                        .withValue("PUBLIC"),
                )
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

sourceSets["main"].kotlin.srcDir("build/generated/jooq/main")

tasks.named("compileKotlin") {
    dependsOn("jooqCodegen")
}

// Gradle 9 strict task validation: ktlint가 generated 디렉토리를 input으로 잡으므로 순서 명시.
tasks.matching { it.name.startsWith("runKtlint") && it.name.endsWith("MainSourceSet") }.configureEach {
    mustRunAfter("jooqCodegen")
    dependsOn("jooqCodegen")
}

openapi3 {
    setServer("http://localhost:8080")
    title = "Starter API"
    description = "Spring Kotlin Starter Kit API"
    version = "0.0.1"
    format = "yaml"
}

// restdocs-api-spec 0.20.x의 OpenApi3Task가 Gradle 9 configuration cache 직렬화에 막힘.
tasks
    .matching { it.name == "openapi3" }
    .configureEach {
        notCompatibleWithConfigurationCache(
            "restdocs-api-spec 0.20.1: OpenApi3Task의 ObjectMapper(StdDateFormat)가 configuration cache 직렬화 미호환",
        )
        dependsOn("test")
    }

tasks.named("build") {
    dependsOn("openapi3")
}

// spec을 main sourceSet output에 등록하면 `test → main classes → copyOpenApiSpec → openapi3 → test`
// circular가 생긴다 → bootJar from()으로만 박고, bootRun은 controller filesystem fallback.
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
