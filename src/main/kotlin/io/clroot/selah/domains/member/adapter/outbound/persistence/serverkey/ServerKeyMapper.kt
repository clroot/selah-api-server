package io.clroot.selah.domains.member.adapter.outbound.persistence.serverkey

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.ServerKey
import io.clroot.selah.domains.member.domain.ServerKeyId
import org.springframework.stereotype.Component

/**
 * ServerKey Domain ↔ Entity 매퍼
 */
@Component
class ServerKeyMapper {
    /**
     * Domain → Entity
     */
    fun toEntity(domain: ServerKey): ServerKeyEntity =
        ServerKeyEntity(
            id = domain.id.value,
            memberId = domain.memberId.value,
            encryptedServerKey = domain.encryptedServerKey,
            iv = domain.iv,
            version = domain.version,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    /**
     * Entity → Domain
     */
    fun toDomain(entity: ServerKeyEntity): ServerKey =
        ServerKey(
            id = ServerKeyId.from(entity.id),
            memberId = MemberId.from(entity.memberId),
            encryptedServerKey = entity.encryptedServerKey,
            iv = entity.iv,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )

    /**
     * 기존 Entity 업데이트 (낙관적 락 유지)
     */
    fun updateEntity(
        entity: ServerKeyEntity,
        domain: ServerKey,
    ) {
        entity.encryptedServerKey = domain.encryptedServerKey
        entity.iv = domain.iv
        entity.updatedAt = domain.updatedAt
    }
}
