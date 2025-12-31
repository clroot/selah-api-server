package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.exception.EncryptionAlreadySetupException
import io.clroot.selah.domains.member.domain.exception.EncryptionSettingsNotFoundException

/**
 * E2E 암호화 설정 관리 UseCase
 *
 * DEK/KEK 구조:
 * - DEK (Data Encryption Key): 실제 데이터 암호화 키
 * - KEK (Key Encryption Key): 로그인 비밀번호에서 파생, DEK 암호화에 사용
 */
interface ManageEncryptionSettingsUseCase {
    /**
     * 암호화 설정 초기화 (회원가입 시)
     *
     * @throws EncryptionAlreadySetupException 이미 설정된 경우
     */
    suspend fun setup(memberId: MemberId, command: SetupEncryptionCommand): EncryptionSettings

    /**
     * 암호화 설정 조회 (로그인 시 DEK 복호화용)
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
     * 복구용 설정 조회 (비밀번호 분실 시 DEK 복구용)
     *
     * @throws EncryptionSettingsNotFoundException 설정이 없는 경우
     */
    suspend fun getRecoverySettings(memberId: MemberId): RecoverySettingsResult

    /**
     * 비밀번호 변경 시 암호화 키 업데이트
     * (새 KEK로 DEK 재암호화)
     *
     * @throws EncryptionSettingsNotFoundException 설정이 없는 경우
     */
    suspend fun updateEncryption(memberId: MemberId, command: UpdateEncryptionCommand): EncryptionSettings

    /**
     * 복구 키 재생성
     * (새 복구 키로 DEK 재암호화)
     *
     * @throws EncryptionSettingsNotFoundException 설정이 없는 경우
     */
    suspend fun updateRecoveryKey(memberId: MemberId, command: UpdateRecoveryKeyCommand): EncryptionSettings

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
    val encryptedDEK: String,
    val recoveryEncryptedDEK: String,
    val recoveryKeyHash: String,
)

/**
 * 암호화 키 업데이트 Command (비밀번호 변경 시)
 */
data class UpdateEncryptionCommand(
    val salt: String,
    val encryptedDEK: String,
)

/**
 * 복구 키 재생성 Command
 */
data class UpdateRecoveryKeyCommand(
    val recoveryEncryptedDEK: String,
    val recoveryKeyHash: String,
)

/**
 * 복구용 설정 Result
 */
data class RecoverySettingsResult(
    val recoveryEncryptedDEK: String,
    val recoveryKeyHash: String,
)
