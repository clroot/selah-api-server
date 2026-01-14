package io.clroot.selah.domains.member.adapter.outbound.persistence.encryption

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * EncryptionSettings Entity Repository
 *
 * 기본 CRUD 작업과 단순 쿼리를 위한 CoroutineCrudRepository 인터페이스입니다.
 * 복잡한 쿼리(save with merge 등)는 JDSL을 통해 PersistenceAdapter에서 처리합니다.
 */
interface EncryptionSettingsEntityRepository : CoroutineCrudRepository<EncryptionSettingsEntity, String> {
    suspend fun findByMemberId(memberId: String): EncryptionSettingsEntity?

    suspend fun existsByMemberId(memberId: String): Boolean

    suspend fun deleteByMemberId(memberId: String)
}
