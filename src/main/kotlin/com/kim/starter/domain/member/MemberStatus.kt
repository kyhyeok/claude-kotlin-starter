package com.kim.starter.domain.member

/**
 * 회원 상태.
 *
 * V1 스키마의 `status VARCHAR(20)` 컬럼과 매핑한다(`@Enumerated(EnumType.STRING)`).
 * 값 추가/제거 시 V1 마이그레이션 또는 후속 마이그레이션의 default 정책을 함께 갱신한다.
 */
enum class MemberStatus {
    /** 가입 직후. 이메일 인증 등 추가 단계 대기. starter kit은 즉시 ACTIVE로 등록한다. */
    PENDING,

    /** 정상 활성 회원. */
    ACTIVE,

    /** 사용자 휴면. */
    INACTIVE,

    /** 정책 위반 등으로 정지. */
    BANNED,
}
