package com.kim.starter.application.auth

import com.kim.starter.application.required.IssuedToken

/**
 * 인증 흐름이 클라이언트에 함께 돌려주는 Access + Refresh Token 묶음.
 *
 * 어댑터(`AuthApi`)에서 응답 DTO로 변환된다.
 */
data class TokenPair(
    val accessToken: IssuedToken,
    val refreshToken: IssuedToken,
)
