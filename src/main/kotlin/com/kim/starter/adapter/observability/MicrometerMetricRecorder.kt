package com.kim.starter.adapter.observability

import com.kim.starter.application.required.MetricRecorder
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

/**
 * [MetricRecorder] Micrometer 어댑터.
 *
 * `MeterRegistry.counter(name, tags...)`는 같은 (name + tags) 조합에 대해 같은 인스턴스를 반환하므로
 * 호출마다 lookup해도 비용이 무시 가능하다. 명시적 캐싱 불필요.
 *
 * Prometheus 노출 시 카운터 이름은 `member_registration_total`(`_total` 접미사는 Micrometer가 자동 부여).
 */
@Component
class MicrometerMetricRecorder(
    private val meterRegistry: MeterRegistry,
) : MetricRecorder {
    override fun recordMemberRegistration(result: MetricRecorder.RegistrationResult) {
        meterRegistry
            .counter(MEMBER_REGISTRATION, "result", result.tag)
            .increment()
    }

    companion object {
        const val MEMBER_REGISTRATION = "member.registration"
    }
}
