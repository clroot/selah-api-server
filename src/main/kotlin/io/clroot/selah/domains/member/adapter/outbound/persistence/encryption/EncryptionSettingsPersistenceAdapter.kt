package io.clroot.selah.domains.member.adapter.outbound.persistence.encryption

import io.clroot.selah.domains.member.application.port.outbound.DeleteEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.LoadEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.SaveEncryptionSettingsPort
import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

/**
 * EncryptionSettings Persistence Adapter
 */
@Component
class EncryptionSettingsPersistenceAdapter(
    private val repository: EncryptionSettingsJpaRepository,
    private val mapper: EncryptionSettingsMapper,
) : LoadEncryptionSettingsPort,
    SaveEncryptionSettingsPort,
    DeleteEncryptionSettingsPort {
    override suspend fun findByMemberId(memberId: MemberId): EncryptionSettings? =
        withContext(Dispatchers.IO) {
            repository.findByMemberId(memberId.value)?.let { mapper.toDomain(it) }
        }

    override suspend fun existsByMemberId(memberId: MemberId): Boolean =
        withContext(Dispatchers.IO) {
            repository.existsByMemberId(memberId.value)
        }

    override suspend fun save(encryptionSettings: EncryptionSettings): EncryptionSettings =
        withContext(Dispatchers.IO) {
            val savedEntity =
                if (repository.existsById(encryptionSettings.id.value)) {
                    // 기존 Entity 업데이트
                    val existingEntity = repository.findById(encryptionSettings.id.value).orElseThrow()
                    mapper.updateEntity(existingEntity, encryptionSettings)
                    repository.save(existingEntity)
                } else {
                    // 새 Entity 생성
                    repository.save(mapper.toEntity(encryptionSettings))
                }

            mapper.toDomain(savedEntity)
        }

    override suspend fun deleteByMemberId(memberId: MemberId): Unit =
        withContext(Dispatchers.IO) {
            repository.deleteByMemberId(memberId.value)
        }
}
