package com.kim.starter.application.auth

import com.kim.starter.application.required.IssuedToken

data class TokenPair(
    val accessToken: IssuedToken,
    val refreshToken: IssuedToken,
)
