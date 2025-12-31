package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.event.EncryptionSettingsDeletedIntegrationEvent
import io.clroot.selah.domains.member.application.port.inbound.ManageEncryptionSettingsUseCase
import io.clroot.selah.domains.member.application.port.inbound.RecoverySettingsResult
import io.clroot.selah.domains.member.application.port.inbound.SetupEncryptionCommand
import io.clroot.selah.domains.member.application.port.inbound.UpdateEncryptionCommand
import io.clroot.selah.domains.member.application.port.inbound.UpdateRecoveryKeyCommand
import io.clroot.selah.domains.member.application.port.outbound.DeleteEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.LoadEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.SaveEncryptionSettingsPort
import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.exception.EncryptionAlreadySetupException
import io.clroot.selah.domains.member.domain.exception.EncryptionSettingsNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * E2E 암호화 설정 서비스
 *
 * DEK/KEK 구조 기반의 암호화 설정을 관리합니다.
 */
@Service
@Transactional
class EncryptionSettingsService(
    private val loadEncryptionSettingsPort: LoadEncryptionSettingsPort,
    private val saveEncryptionSettingsPort: SaveEncryptionSettingsPort,
    private val deleteEncryptionSettingsPort: DeleteEncryptionSettingsPort,
    private val eventPublisher: ApplicationEventPublisher,
) : ManageEncryptionSettingsUseCase {

    override suspend fun setup(memberId: MemberId, command: SetupEncryptionCommand): EncryptionSettings {
        // 이미 설정되어 있는지 확인
        if (loadEncryptionSettingsPort.existsByMemberId(memberId)) {
            throw EncryptionAlreadySetupException(memberId.value)
        }

        // 암호화 설정 생성
        val encryptionSettings = EncryptionSettings.create(
            memberId = memberId,
            salt = command.salt,
            encryptedDEK = command.encryptedDEK,
            recoveryEncryptedDEK = command.recoveryEncryptedDEK,
            recoveryKeyHash = command.recoveryKeyHash,
        )

        // 저장
        return saveEncryptionSettingsPort.save(encryptionSettings)
    }

    @Transactional(readOnly = true)
    override suspend fun getSettings(memberId: MemberId): EncryptionSettings {
        return loadEncryptionSettingsPort.findByMemberId(memberId)
            ?: throw EncryptionSettingsNotFoundException(memberId.value)
    }

    @Transactional(readOnly = true)
    override suspend fun hasSettings(memberId: MemberId): Boolean {
        return loadEncryptionSettingsPort.existsByMemberId(memberId)
    }

    @Transactional(readOnly = true)
    override suspend fun verifyRecoveryKey(memberId: MemberId, recoveryKeyHash: String): Boolean {
        val settings = loadEncryptionSettingsPort.findByMemberId(memberId)
            ?: throw EncryptionSettingsNotFoundException(memberId.value)

        return settings.recoveryKeyHash == recoveryKeyHash
    }

    @Transactional(readOnly = true)
    override suspend fun getRecoverySettings(memberId: MemberId): RecoverySettingsResult {
        val settings = loadEncryptionSettingsPort.findByMemberId(memberId)
            ?: throw EncryptionSettingsNotFoundException(memberId.value)

        return RecoverySettingsResult(
            recoveryEncryptedDEK = settings.recoveryEncryptedDEK,
            recoveryKeyHash = settings.recoveryKeyHash,
        )
    }

    override suspend fun updateEncryption(
        memberId: MemberId,
        command: UpdateEncryptionCommand,
    ): EncryptionSettings {
        val settings = loadEncryptionSettingsPort.findByMemberId(memberId)
            ?: throw EncryptionSettingsNotFoundException(memberId.value)

        settings.updateEncryption(
            newSalt = command.salt,
            newEncryptedDEK = command.encryptedDEK,
        )

        return saveEncryptionSettingsPort.save(settings)
    }

    override suspend fun updateRecoveryKey(
        memberId: MemberId,
        command: UpdateRecoveryKeyCommand,
    ): EncryptionSettings {
        val settings = loadEncryptionSettingsPort.findByMemberId(memberId)
            ?: throw EncryptionSettingsNotFoundException(memberId.value)

        settings.updateRecoveryKey(
            newRecoveryEncryptedDEK = command.recoveryEncryptedDEK,
            newRecoveryKeyHash = command.recoveryKeyHash,
        )

        return saveEncryptionSettingsPort.save(settings)
    }

    override suspend fun deleteSettings(memberId: MemberId) {
        // 암호화 설정 존재 확인
        if (!loadEncryptionSettingsPort.existsByMemberId(memberId)) {
            throw EncryptionSettingsNotFoundException(memberId.value)
        }

        // 암호화 설정 삭제
        deleteEncryptionSettingsPort.deleteByMemberId(memberId)

        // Integration Event 발행 (Prayer 도메인에서 관련 데이터 삭제)
        eventPublisher.publishEvent(
            EncryptionSettingsDeletedIntegrationEvent(memberId = memberId.value)
        )
    }
}
