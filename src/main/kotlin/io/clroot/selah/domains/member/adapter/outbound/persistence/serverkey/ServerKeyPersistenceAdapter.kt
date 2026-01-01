package io.clroot.selah.domains.member.adapter.outbound.persistence.serverkey

import io.clroot.selah.domains.member.application.port.outbound.DeleteServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.LoadServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.SaveServerKeyPort
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.ServerKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

/**
 * ServerKey Persistence Adapter
 */
@Component
class ServerKeyPersistenceAdapter(
    private val repository: ServerKeyJpaRepository,
    private val mapper: ServerKeyMapper,
) : LoadServerKeyPort,
    SaveServerKeyPort,
    DeleteServerKeyPort {
    override suspend fun findByMemberId(memberId: MemberId): ServerKey? =
        withContext(Dispatchers.IO) {
            repository.findByMemberId(memberId.value)?.let { mapper.toDomain(it) }
        }

    override suspend fun existsByMemberId(memberId: MemberId): Boolean =
        withContext(Dispatchers.IO) {
            repository.existsByMemberId(memberId.value)
        }

    override suspend fun save(serverKey: ServerKey): ServerKey =
        withContext(Dispatchers.IO) {
            val savedEntity =
                if (repository.existsById(serverKey.id.value)) {
                    // 기존 Entity 업데이트
                    val existingEntity = repository.findById(serverKey.id.value).orElseThrow()
                    mapper.updateEntity(existingEntity, serverKey)
                    repository.save(existingEntity)
                } else {
                    // 새 Entity 생성
                    repository.save(mapper.toEntity(serverKey))
                }

            mapper.toDomain(savedEntity)
        }

    override suspend fun deleteByMemberId(memberId: MemberId): Unit =
        withContext(Dispatchers.IO) {
            repository.deleteByMemberId(memberId.value)
        }
}
