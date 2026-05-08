package com.kim.starter.support.fixture

import com.kim.starter.domain.member.Member
import com.kim.starter.support.generator.EmailGenerator.generateEmail
import java.time.OffsetDateTime
import java.time.ZoneOffset

object MemberFixture {
    private val DEFAULT_NOW: OffsetDateTime
        get() = OffsetDateTime.of(2026, 5, 7, 0, 0, 0, 0, ZoneOffset.UTC)

    fun member(
        email: String = generateEmail(),
        passwordHash: String = "hashed-pwd",
        now: OffsetDateTime = DEFAULT_NOW,
    ): Member = Member.register(email, passwordHash, now)
}
