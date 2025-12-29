package io.clroot.selah.domains.member.adapter.outbound.persistence.apikey

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * API Key JPA Repository
 */
@Repository
interface ApiKeyJpaRepository : JpaRepository<ApiKeyEntity, String> {
    /**
     * 회원의 모든 API Key를 조회합니다.
     */
    fun findAllByMemberId(memberId: String): List<ApiKeyEntity>

    /**
     * 키 해시로 API Key를 조회합니다.
     */
    fun findByKeyHash(keyHash: String): ApiKeyEntity?
}
