package io.clroot.selah.domains.member.adapter.outbound.persistence.apikey

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * ApiKey Entity Repository
 *
 * 기본 CRUD 작업과 단순 쿼리를 위한 CoroutineCrudRepository 인터페이스입니다.
 * 복잡한 쿼리(updateLastUsedAt 등)는 JDSL을 통해 PersistenceAdapter에서 처리합니다.
 */
interface ApiKeyEntityRepository : CoroutineCrudRepository<ApiKeyEntity, String> {
    suspend fun findAllByMemberId(memberId: String): List<ApiKeyEntity>

    suspend fun findByKeyHash(keyHash: String): ApiKeyEntity?
}
