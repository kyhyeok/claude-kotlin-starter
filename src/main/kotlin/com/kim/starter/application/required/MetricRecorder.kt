package com.kim.starter.application.required

/**
 * 도메인 메트릭 기록 포트.
 *
 * 애플리케이션 계층은 이 인터페이스만 의존한다. Micrometer 같은 구체 라이브러리는
 * `adapter/observability/`에서 구현(ADR-0016).
 *
 * 헥사고날 의존 단방향 + "외부는 모두 포트 뒤"(CLAUDE.md §1, §4)에 정렬한다.
 * 기본 빈 구현([NoopMetricRecorder])은 단위 테스트와 메트릭 레지스트리 미가용 환경에서 사용.
 */
interface MetricRecorder {
    fun recordMemberRegistration(result: RegistrationResult)

    /**
     * 회원 등록 결과 — Prometheus 카운터의 `result` 태그 값으로 노출된다.
     * 새 결과를 추가하면 운영 대시보드 쿼리도 함께 갱신할 것.
     */
    enum class RegistrationResult(
        val tag: String,
    ) {
        SUCCESS("success"),
        DUPLICATE("duplicate"),
    }
}
