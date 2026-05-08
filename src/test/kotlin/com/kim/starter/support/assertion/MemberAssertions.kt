package com.kim.starter.support.assertion

import com.kim.starter.domain.member.Member
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import java.time.OffsetDateTime

object MemberAssertions {
    fun hasEmail(email: String): ThrowingConsumer<Member> =
        ThrowingConsumer { member ->
            assertThat(member.email).isEqualTo(email)
        }

    fun isActive(): ThrowingConsumer<Member> =
        ThrowingConsumer { member ->
            assertThat(member.isActive).isTrue()
        }

    fun hasTimestamps(
        createdAt: OffsetDateTime,
        updatedAt: OffsetDateTime = createdAt,
    ): ThrowingConsumer<Member> =
        ThrowingConsumer { member ->
            assertThat(member.createdAt).isEqualTo(createdAt)
            assertThat(member.updatedAt).isEqualTo(updatedAt)
        }
}
