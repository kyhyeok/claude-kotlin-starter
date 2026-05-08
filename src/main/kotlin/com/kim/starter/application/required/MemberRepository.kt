package com.kim.starter.application.required

import com.kim.starter.domain.member.Member

// JpaRepository 메서드가 도메인에 새지 않도록 분리 (CLAUDE.md §4).
interface MemberRepository {
    fun save(member: Member): Member

    fun findByEmail(email: String): Member?

    fun findById(id: Long): Member?

    fun existsByEmail(email: String): Boolean
}
