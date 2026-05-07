package com.kim.starter.application.auth.provided

import com.kim.starter.application.auth.TokenPair

/**
 * Refresh Token으로 새 Access/Refresh Token 쌍을 발급하는 Use Case 포트(provided).
 *
 * Rotation 정책: 매 호출마다 RT 신규 발급 + Redis 활성 RT 교체 → 이전 RT 무효.
 */
interface TokenRefresher {
    fun refresh(refreshToken: String): TokenPair
}
