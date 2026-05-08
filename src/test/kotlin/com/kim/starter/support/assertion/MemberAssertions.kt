package com.kim.starter.support.assertion

import com.kim.starter.domain.member.Email
import com.kim.starter.domain.member.Member
import com.kim.starter.domain.member.MemberStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import java.time.OffsetDateTime

/**
 * Member 도메인 단언 (CLAUDE.md §5).
 *
 * AssertJ의 `satisfies(...)`와 조합하여 한 줄로 도메인 객체의 주요 필드를 검증한다.
 * 필드별 `assertThat`을 분기하는 boilerplate를 줄이고 의도를 한 곳에 모은다.
 *
 * 사용:
 * ```
 * assertThat(member).satisfies(
 *     MemberAssertions.isActiveWith(email),
 *     MemberAssertions.hasTimestamps(now),
 * )
 * ```
 */
object MemberAssertions {
    fun isActiveWith(email: Email): ThrowingConsumer<Member> =
        ThrowingConsumer { member ->
            assertThat(member.email).isEqualTo(email)
            assertThat(member.currentStatus).isEqualTo(MemberStatus.ACTIVE)
            assertThat(member.isActive).isTrue()
        }

    fun isInStatus(status: MemberStatus): ThrowingConsumer<Member> =
        ThrowingConsumer { member ->
            assertThat(member.currentStatus).isEqualTo(status)
            assertThat(member.isActive).isEqualTo(status == MemberStatus.ACTIVE)
        }

    fun hasTimestamps(
        createdAt: OffsetDateTime,
        updatedAt: OffsetDateTime = createdAt,
    ): ThrowingConsumer<Member> =
        ThrowingConsumer { member ->
            assertThat(member.createdAt).isEqualTo(createdAt)
            assertThat(member.updatedAtValue).isEqualTo(updatedAt)
        }
}
