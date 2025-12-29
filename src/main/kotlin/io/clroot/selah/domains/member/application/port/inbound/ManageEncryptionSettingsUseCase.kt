package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.exception.EncryptionAlreadySetupException
import io.clroot.selah.domains.member.domain.exception.EncryptionSettingsNotFoundException

/**
 * E2E 암호화 설정 관리 UseCase
 */
interface ManageEncryptionSettingsUseCase {
    /**
     * 암호화 설정 초기화
     *
     * @throws EncryptionAlreadySetupException 이미 설정된 경우
     */
    suspend fun setup(memberId: MemberId, command: SetupEncryptionCommand): EncryptionSettings

    /**
     * 암호화 설정 조회
     *
     * @throws EncryptionSettingsNotFoundException 설정이 없는 경우
     */
    suspend fun getSettings(memberId: MemberId): EncryptionSettings

    /**
     * 암호화 설정 존재 여부 확인
     */
    suspend fun hasSettings(memberId: MemberId): Boolean

    /**
     * 복구 키 검증
     *
     * @throws EncryptionSettingsNotFoundException 설정이 없는 경우
     */
    suspend fun verifyRecoveryKey(memberId: MemberId, recoveryKeyHash: String): Boolean

    /**
     * 암호화 설정 삭제
     * EncryptionSettingsDeletedEvent를 발행하여 Prayer 도메인에서
     * 관련 데이터를 삭제하도록 합니다.
     *
     * @throws EncryptionSettingsNotFoundException 설정이 없는 경우
     */
    suspend fun deleteSettings(memberId: MemberId)
}

/**
 * 암호화 설정 초기화 Command
 */
data class SetupEncryptionCommand(
    val salt: String,
    val recoveryKeyHash: String,
)
