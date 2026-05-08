package com.kim.starter.application.auth.provided

import com.kim.starter.domain.member.Member

interface MemberRegister {
    fun register(command: RegisterCommand): Member

    data class RegisterCommand(
        val email: String,
        val rawPassword: String,
    )
}
