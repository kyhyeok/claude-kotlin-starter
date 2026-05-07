package com.kim.starter.domain.member

/**
 * 이메일 VO.
 *
 * - `init`에서 즉시 검증 → 잘못된 값으로 도메인이 만들어지는 경로를 차단(CLAUDE.md §2).
 * - 길이 제한은 V1 스키마(`VARCHAR(255)`)와 동기화.
 *
 * Hibernate 7 시점에 Kotlin `@JvmInline value class`의 `@Embeddable` 호환성이
 * 안정적이지 않아 Member 엔티티는 String 컬럼으로 보관하고 도메인 메서드에서 [Email]로 노출한다.
 */
@JvmInline
value class Email(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "이메일은 비어있을 수 없습니다" }
        require(value.length <= MAX_LENGTH) { "이메일은 ${MAX_LENGTH}자 이하여야 합니다: ${value.length}" }
        require(EMAIL_PATTERN.matches(value)) { "올바른 이메일 형식이 아닙니다: $value" }
    }

    companion object {
        const val MAX_LENGTH = 255

        // RFC 5322의 완전한 정규식은 비현실적으로 길어 실용적인 범위로 제한.
        private val EMAIL_PATTERN =
            Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
