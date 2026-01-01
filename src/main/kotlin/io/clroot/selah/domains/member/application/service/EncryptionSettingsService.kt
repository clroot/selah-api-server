package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.event.EncryptionSettingsDeletedIntegrationEvent
import io.clroot.selah.domains.member.application.port.inbound.EncryptionSettingsWithServerKey
import io.clroot.selah.domains.member.application.port.inbound.ManageEncryptionSettingsUseCase
import io.clroot.selah.domains.member.application.port.inbound.RecoverySettingsResult
import io.clroot.selah.domains.member.application.port.inbound.SetupEncryptionCommand
import io.clroot.selah.domains.member.application.port.inbound.SetupEncryptionResult
import io.clroot.selah.domains.member.application.port.inbound.UpdateEncryptionCommand
import io.clroot.selah.domains.member.application.port.inbound.UpdateEncryptionResult
import io.clroot.selah.domains.member.application.port.inbound.UpdateRecoveryKeyCommand
import io.clroot.selah.domains.member.application.port.outbound.DeleteEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.DeleteServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.LoadEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.LoadServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.SaveEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.SaveServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.ServerKeyEncryptionPort
import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.ServerKey
import io.clroot.selah.domains.member.domain.exception.EncryptionAlreadySetupException
import io.clroot.selah.domains.member.domain.exception.EncryptionSettingsNotFoundException
import io.clroot.selah.domains.member.domain.exception.ServerKeyNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * E2E 암호화 설정 서비스
 *
 * 키 구조:
 * - DEK: 실제 데이터 암호화 키
 * - Client KEK: 6자리 PIN에서 파생
 * - Server Key: 서버에서 생성, Master Key로 암호화하여 저장
 * - Combined KEK = HKDF(Client KEK + Server Key): DEK 암호화에 사용
 */
@Service
@Transactional
class EncryptionSettingsService(
    private val loadEncryptionSettingsPort: LoadEncryptionSettingsPort,
    private val saveEncryptionSettingsPort: SaveEncryptionSettingsPort,
    private val deleteEncryptionSettingsPort: DeleteEncryptionSettingsPort,
    private val loadServerKeyPort: LoadServerKeyPort,
    private val saveServerKeyPort: SaveServerKeyPort,
    private val deleteServerKeyPort: DeleteServerKeyPort,
    private val serverKeyEncryptionPort: ServerKeyEncryptionPort,
    private val eventPublisher: ApplicationEventPublisher,
) : ManageEncryptionSettingsUseCase {
    override suspend fun setup(
        memberId: MemberId,
        command: SetupEncryptionCommand,
    ): SetupEncryptionResult {
        // 이미 설정되어 있는지 확인
        if (loadEncryptionSettingsPort.existsByMemberId(memberId)) {
            throw EncryptionAlreadySetupException(memberId.value)
        }

        // Server Key 생성 및 암호화
        val serverKeyResult = serverKeyEncryptionPort.generateAndEncryptServerKey()

        // Server Key 저장
        val serverKey =
            ServerKey.create(
                memberId = memberId,
                encryptedServerKey = serverKeyResult.encryptedServerKey,
                iv = serverKeyResult.iv,
            )
        saveServerKeyPort.save(serverKey)

        // 암호화 설정 생성 및 저장
        val encryptionSettings =
            EncryptionSettings.create(
                memberId = memberId,
                salt = command.salt,
                encryptedDEK = command.encryptedDEK,
                recoveryEncryptedDEK = command.recoveryEncryptedDEK,
                recoveryKeyHash = command.recoveryKeyHash,
            )
        val savedSettings = saveEncryptionSettingsPort.save(encryptionSettings)

        return SetupEncryptionResult(
            settings = savedSettings,
            serverKey = serverKeyResult.plainServerKey,
        )
    }

    @Transactional(readOnly = true)
    override suspend fun getSettings(memberId: MemberId): EncryptionSettingsWithServerKey {
        val settings =
            loadEncryptionSettingsPort.findByMemberId(memberId)
                ?: throw EncryptionSettingsNotFoundException(memberId.value)

        val serverKey =
            loadServerKeyPort.findByMemberId(memberId)
                ?: throw ServerKeyNotFoundException(memberId.value)

        // Server Key 복호화
        val plainServerKey =
            serverKeyEncryptionPort.decryptServerKey(
                encryptedServerKey = serverKey.encryptedServerKey,
                iv = serverKey.iv,
            )

        return EncryptionSettingsWithServerKey(
            salt = settings.salt,
            encryptedDEK = settings.encryptedDEK,
            serverKey = plainServerKey,
        )
    }

    @Transactional(readOnly = true)
    override suspend fun hasSettings(memberId: MemberId): Boolean = loadEncryptionSettingsPort.existsByMemberId(memberId)

    @Transactional(readOnly = true)
    override suspend fun verifyRecoveryKey(
        memberId: MemberId,
        recoveryKeyHash: String,
    ): Boolean {
        val settings =
            loadEncryptionSettingsPort.findByMemberId(memberId)
                ?: throw EncryptionSettingsNotFoundException(memberId.value)

        return settings.recoveryKeyHash == recoveryKeyHash
    }

    @Transactional(readOnly = true)
    override suspend fun getRecoverySettings(memberId: MemberId): RecoverySettingsResult {
        val settings =
            loadEncryptionSettingsPort.findByMemberId(memberId)
                ?: throw EncryptionSettingsNotFoundException(memberId.value)

        return RecoverySettingsResult(
            recoveryEncryptedDEK = settings.recoveryEncryptedDEK,
            recoveryKeyHash = settings.recoveryKeyHash,
        )
    }

    override suspend fun updateEncryption(
        memberId: MemberId,
        command: UpdateEncryptionCommand,
    ): UpdateEncryptionResult {
        val settings =
            loadEncryptionSettingsPort.findByMemberId(memberId)
                ?: throw EncryptionSettingsNotFoundException(memberId.value)

        val existingServerKey =
            loadServerKeyPort.findByMemberId(memberId)
                ?: throw ServerKeyNotFoundException(memberId.value)

        val plainServerKey =
            if (command.rotateServerKey) {
                // PIN 변경: 새 Server Key 생성
                val serverKeyResult = serverKeyEncryptionPort.generateAndEncryptServerKey()
                existingServerKey.updateServerKey(
                    newEncryptedServerKey = serverKeyResult.encryptedServerKey,
                    newIv = serverKeyResult.iv,
                )
                saveServerKeyPort.save(existingServerKey)
                serverKeyResult.plainServerKey
            } else {
                // 초기 설정 완료 또는 Server Key 유지: 기존 Server Key 복호화
                serverKeyEncryptionPort.decryptServerKey(
                    encryptedServerKey = existingServerKey.encryptedServerKey,
                    iv = existingServerKey.iv,
                )
            }

        // 암호화 설정 업데이트
        settings.updateEncryption(
            newSalt = command.salt,
            newEncryptedDEK = command.encryptedDEK,
        )
        val savedSettings = saveEncryptionSettingsPort.save(settings)

        return UpdateEncryptionResult(
            settings = savedSettings,
            serverKey = plainServerKey,
        )
    }

    override suspend fun updateRecoveryKey(
        memberId: MemberId,
        command: UpdateRecoveryKeyCommand,
    ): EncryptionSettings {
        val settings =
            loadEncryptionSettingsPort.findByMemberId(memberId)
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

        // Server Key 삭제
        deleteServerKeyPort.deleteByMemberId(memberId)

        // Integration Event 발행 (Prayer 도메인에서 관련 데이터 삭제)
        eventPublisher.publishEvent(
            EncryptionSettingsDeletedIntegrationEvent(memberId = memberId.value),
        )
    }
}
