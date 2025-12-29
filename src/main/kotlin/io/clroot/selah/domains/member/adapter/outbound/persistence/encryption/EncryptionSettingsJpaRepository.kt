package io.clroot.selah.domains.member.adapter.outbound.persistence.encryption

import org.springframework.data.jpa.repository.JpaRepository

/**
 * EncryptionSettings JPA Repository
 */
interface EncryptionSettingsJpaRepository : JpaRepository<EncryptionSettingsEntity, String> {
    fun findByMemberId(memberId: String): EncryptionSettingsEntity?
    fun existsByMemberId(memberId: String): Boolean
    fun deleteByMemberId(memberId: String)
}
