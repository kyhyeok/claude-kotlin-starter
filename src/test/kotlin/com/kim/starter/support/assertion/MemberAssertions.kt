package com.kim.starter.support.assertion

import com.kim.starter.domain.member.Member
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import java.time.OffsetDateTime

/**
 * Member 도메인 단언 (CLAUDE.md §5).
 *
 * AssertJ의 `satisfies(...)`와 조합하여 한 줄로 도메인 객체의 주요 필드를 검증한다.
 *
 * 사용:
 * ```
 * assertThat(member).satisfies(
 *     MemberAssertions.hasEmail(email),
 *     MemberAssertions.hasTimestamps(now),
 * )
 * ```
 */
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
