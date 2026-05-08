package com.kim.starter

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.Architectures.layeredArchitecture

// 의존 방향 강제: adapter → application → domain. 역방향 시 PR 머지 차단.
@AnalyzeClasses(packages = ["com.kim.starter"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {
    @ArchTest
    val hexagonal: ArchRule =
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("domain")
            .definedBy("com.kim.starter.domain..")
            .layer("application")
            .definedBy("com.kim.starter.application..")
            .layer("adapter")
            .definedBy("com.kim.starter.adapter..")
            .layer("config")
            .definedBy("com.kim.starter.config..")
            .whereLayer("domain")
            .mayOnlyBeAccessedByLayers("application", "adapter", "config")
            .whereLayer("application")
            .mayOnlyBeAccessedByLayers("adapter", "config")
            .whereLayer("adapter")
            .mayNotBeAccessedByAnyLayer()
            // ArchUnit 1.4.x는 빈 레이어를 strict 거부 → starter는 도메인이 비어있을 수도 있어 옵트아웃.
            .withOptionalLayers(true)
}
