package io.clroot.selah.domains.member.adapter.outbound.persistence.encryption

import io.clroot.selah.domains.member.application.port.outbound.DeleteEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.LoadEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.SaveEncryptionSettingsPort
import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId
import org.springframework.stereotype.Component

/**
 * EncryptionSettings Persistence Adapter
 *
 * CoroutineCrudRepository를 사용하여 CRUD 작업을 처리합니다.
 */
@Component
class EncryptionSettingsPersistenceAdapter(
    private val repository: EncryptionSettingsEntityRepository,
    private val mapper: EncryptionSettingsMapper,
) : LoadEncryptionSettingsPort,
    SaveEncryptionSettingsPort,
    DeleteEncryptionSettingsPort {
    override suspend fun findByMemberId(memberId: MemberId): EncryptionSettings? =
        repository.findByMemberId(memberId.value)?.let { mapper.toDomain(it) }

    override suspend fun existsByMemberId(memberId: MemberId): Boolean = repository.existsByMemberId(memberId.value)

    override suspend fun save(encryptionSettings: EncryptionSettings): EncryptionSettings {
        val saved = repository.save(mapper.toEntity(encryptionSettings))
        return mapper.toDomain(saved)
    }

    override suspend fun deleteByMemberId(memberId: MemberId) {
        repository.deleteByMemberId(memberId.value)
    }
}
