package com.kim.starter.application.required

import com.kim.starter.domain.member.Email
import com.kim.starter.domain.member.Member

/**
 * 회원 도메인 영속성 포트(헥사고날 required port).
 *
 * - `JpaRepository` 메서드(`findAll`, `Pageable` 등)가 도메인에 새지 않도록 분리한다(CLAUDE.md §4).
 * - 어댑터(`adapter.persistence.JpaMemberRepository`)가 Spring Data JPA 위임으로 구현한다.
 * - 시간/시점은 호출자가 [Member.register] 등 도메인 팩토리 단계에서 결정한다.
 */
interface MemberRepository {
    fun save(member: Member): Member

    fun findByEmail(email: Email): Member?

    fun findById(id: Long): Member?

    fun existsByEmail(email: Email): Boolean
}
