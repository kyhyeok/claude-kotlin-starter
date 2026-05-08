package com.kim.starter.adapter.persistence

import com.kim.starter.domain.member.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberSpringDataRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Member?

    fun existsByEmail(email: String): Boolean
}
