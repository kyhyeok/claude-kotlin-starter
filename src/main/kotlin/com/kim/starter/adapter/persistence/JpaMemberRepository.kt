package com.kim.starter.adapter.persistence

import com.kim.starter.application.required.MemberRepository
import com.kim.starter.domain.member.Member
import org.springframework.stereotype.Component

@Component
class JpaMemberRepository(
    private val delegate: MemberSpringDataRepository,
) : MemberRepository {
    override fun save(member: Member): Member = delegate.save(member)

    override fun findByEmail(email: String): Member? = delegate.findByEmail(email)

    override fun findById(id: Long): Member? = delegate.findById(id).orElse(null)

    override fun existsByEmail(email: String): Boolean = delegate.existsByEmail(email)
}
