package com.kim.starter.application.auth.provided

import com.kim.starter.application.auth.TokenPair

interface LoginAuthenticator {
    fun login(command: LoginCommand): TokenPair

    data class LoginCommand(
        val email: String,
        val rawPassword: String,
    )
}
