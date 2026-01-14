package io.clroot.selah.domains.member.adapter.outbound.persistence.serverkey

import io.clroot.selah.domains.member.application.port.outbound.DeleteServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.LoadServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.SaveServerKeyPort
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.ServerKey
import org.springframework.stereotype.Component

/**
 * ServerKey Persistence Adapter
 *
 * CoroutineCrudRepository를 사용하여 CRUD 작업을 처리합니다.
 */
@Component
class ServerKeyPersistenceAdapter(
    private val repository: ServerKeyEntityRepository,
    private val mapper: ServerKeyMapper,
) : LoadServerKeyPort,
    SaveServerKeyPort,
    DeleteServerKeyPort {
    override suspend fun findByMemberId(memberId: MemberId): ServerKey? =
        repository.findByMemberId(memberId.value)?.let { mapper.toDomain(it) }

    override suspend fun existsByMemberId(memberId: MemberId): Boolean =
        repository.existsByMemberId(memberId.value)

    override suspend fun save(serverKey: ServerKey): ServerKey {
        val saved = repository.save(mapper.toEntity(serverKey))
        return mapper.toDomain(saved)
    }

    override suspend fun deleteByMemberId(memberId: MemberId) {
        repository.deleteByMemberId(memberId.value)
    }
}
