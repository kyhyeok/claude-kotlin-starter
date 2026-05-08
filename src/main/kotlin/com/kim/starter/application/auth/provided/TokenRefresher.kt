package com.kim.starter.application.auth.provided

import com.kim.starter.application.auth.TokenPair

interface TokenRefresher {
    fun refresh(refreshToken: String): TokenPair
}
