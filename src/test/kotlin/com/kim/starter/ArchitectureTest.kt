package com.kim.starter

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.Architectures.layeredArchitecture

/**
 * 헥사고날 아키텍처 의존 방향 강제.
 *
 * 의존 방향: adapter → application → domain (역방향 금지).
 *
 * 이 테스트가 깨지면 PR이 머지될 수 없다.
 * 새 패키지를 추가하려면 layer를 명확히 결정한 후 이 규칙을 갱신할 것.
 *
 * `withOptionalLayers(true)`: starter kit은 도메인이 비어있는 상태로 시작하므로
 * 빈 레이어를 허용한다. 사용자가 첫 도메인을 추가하면 그때부터 룰이 실 검증한다.
 * (ArchUnit 1.4.x부터 기본 strict — 빈 레이어 = 실패. 이 옵션으로 옵트아웃)
 */
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
            .withOptionalLayers(true)
}
