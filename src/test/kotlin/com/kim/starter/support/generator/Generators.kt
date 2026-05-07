package com.kim.starter.support.generator

import java.security.SecureRandom
import java.util.UUID

/**
 * 테스트 데이터 generator.
 *
 * 각 테스트가 완전히 독립된 데이터를 사용하도록 → @Transactional/deleteAll 없이 격리.
 *
 * 새 도메인 추가 시 여기에 generator를 함께 추가.
 */
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
