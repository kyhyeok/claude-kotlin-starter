package com.kim.starter.adapter.persistence

import com.kim.starter.application.required.MemberRepository
import com.kim.starter.domain.member.Member
import org.springframework.stereotype.Component

/**
 * [MemberRepository] 포트의 Spring Data JPA 어댑터.
 *
 * 단순 위임이므로 별도 매핑 로직이 필요 없다. 복잡한 조회는 jOOQ를 사용한 별도
 * `MemberFinder`(application/required) 포트로 분리할 수 있다(CLAUDE.md `JPA 쓰기 + jOOQ 읽기`).
 */
@Component
class JpaMemberRepository(
    private val delegate: MemberSpringDataRepository,
) : MemberRepository {
    override fun save(member: Member): Member = delegate.save(member)

    override fun findByEmail(email: String): Member? = delegate.findByEmail(email)

    override fun findById(id: Long): Member? = delegate.findById(id).orElse(null)

    override fun existsByEmail(email: String): Boolean = delegate.existsByEmail(email)
}
