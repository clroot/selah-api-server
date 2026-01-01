package io.clroot.selah.domains.member.adapter.outbound.persistence.encryption

import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.EncryptionSettingsId
import io.clroot.selah.domains.member.domain.MemberId
import org.springframework.stereotype.Component

/**
 * EncryptionSettings Domain ↔ Entity 매퍼
 */
@Component
class EncryptionSettingsMapper {
    /**
     * Domain → Entity
     */
    fun toEntity(domain: EncryptionSettings): EncryptionSettingsEntity =
        EncryptionSettingsEntity(
            id = domain.id.value,
            memberId = domain.memberId.value,
            salt = domain.salt,
            encryptedDek = domain.encryptedDEK,
            recoveryEncryptedDek = domain.recoveryEncryptedDEK,
            recoveryKeyHash = domain.recoveryKeyHash,
            version = domain.version,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    /**
     * Entity → Domain
     */
    fun toDomain(entity: EncryptionSettingsEntity): EncryptionSettings =
        EncryptionSettings(
            id = EncryptionSettingsId.from(entity.id),
            memberId = MemberId.from(entity.memberId),
            salt = entity.salt,
            encryptedDEK = entity.encryptedDek,
            recoveryEncryptedDEK = entity.recoveryEncryptedDek,
            recoveryKeyHash = entity.recoveryKeyHash,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )

    /**
     * 기존 Entity 업데이트 (낙관적 락 유지)
     */
    fun updateEntity(
        entity: EncryptionSettingsEntity,
        domain: EncryptionSettings,
    ) {
        entity.salt = domain.salt
        entity.encryptedDek = domain.encryptedDEK
        entity.recoveryEncryptedDek = domain.recoveryEncryptedDEK
        entity.recoveryKeyHash = domain.recoveryKeyHash
        entity.updatedAt = domain.updatedAt
    }
}
