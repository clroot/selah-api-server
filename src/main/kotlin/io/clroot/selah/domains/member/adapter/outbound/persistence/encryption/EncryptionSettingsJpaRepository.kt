package io.clroot.selah.domains.member.adapter.outbound.persistence.encryption

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional

/**
 * EncryptionSettings JPA Repository
 */
interface EncryptionSettingsJpaRepository : JpaRepository<EncryptionSettingsEntity, String> {
    fun findByMemberId(memberId: String): EncryptionSettingsEntity?

    fun existsByMemberId(memberId: String): Boolean

    @Modifying
    @Transactional
    fun deleteByMemberId(memberId: String)
}
