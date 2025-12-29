package io.clroot.selah.domains.member.adapter.outbound.persistence.member

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Member JPA Repository
 *
 * Spring Data JPA 기본 기능만 제공합니다.
 * 복잡한 쿼리는 MemberPersistenceAdapter에서 JDSL로 직접 구현합니다.
 */
@Repository
interface MemberJpaRepository : JpaRepository<MemberEntity, String> {
    /**
     * 이메일 존재 여부를 확인합니다.
     */
    fun existsByEmail(email: String): Boolean
}
