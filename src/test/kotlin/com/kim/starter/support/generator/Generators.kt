package com.kim.starter.support.generator

import java.security.SecureRandom
import java.util.UUID

object EmailGenerator {
    fun generateEmail(): String = "${UUID.randomUUID()}@test.com"
}

object UsernameGenerator {
    fun generateUsername(): String = "user${UUID.randomUUID()}"
}

object PasswordGenerator {
    private val random = SecureRandom()

    fun generatePassword(): String {
        val sb = StringBuilder("Pwd")
        repeat(10) {
            sb.append(('A'..'Z').random())
            sb.append(('0'..'9').random())
            sb.append(('a'..'z').random())
        }
        return sb.toString()
    }
}
