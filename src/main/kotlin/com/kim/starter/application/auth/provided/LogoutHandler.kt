package com.kim.starter.application.auth.provided

/**
 * 로그아웃 Use Case 포트(provided) — 활성 Refresh Token을 즉시 폐기.
 *
 * Access Token은 stateless이므로 만료 전까지 유효하다. starter kit은 AT 블랙리스트를 운영하지 않는다.
 * (필요해지면 Redis 기반 jti 블랙리스트를 추가)
 */
interface LogoutHandler {
    fun logout(memberId: Long)
}
