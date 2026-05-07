package com.kim.starter.adapter.persistence

import com.kim.starter.domain.member.Member
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Spring Data JPA 직접 구현체.
 *
 * 어댑터 패키지 외부(application/domain)에서는 직접 의존하지 않는다 →
 * 애플리케이션/도메인은 [com.kim.starter.application.required.MemberRepository] 포트만 사용.
 * ArchUnit 룰(`adapter` 레이어는 외부에서 접근 불가)로 의존 방향을 강제한다.
 */
interface MemberSpringDataRepository : JpaRepository<Member, Long> {
    fun findByEmailValue(emailValue: String): Member?

    fun existsByEmailValue(emailValue: String): Boolean
}
