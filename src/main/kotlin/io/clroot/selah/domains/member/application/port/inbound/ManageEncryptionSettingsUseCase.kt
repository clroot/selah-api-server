package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.exception.EncryptionAlreadySetupException
import io.clroot.selah.domains.member.domain.exception.EncryptionSettingsNotFoundException

/**
 * E2E 암호화 설정 관리 UseCase
 *
 * 키 구조:
 * - DEK (Data Encryption Key): 실제 데이터 암호화 키
 * - Client KEK: 6자리 PIN에서 파생
 * - Server Key: 서버에서 생성, Master Key로 암호화하여 저장
 * - Combined KEK = HKDF(Client KEK + Server Key): DEK 암호화에 사용
 */
interface ManageEncryptionSettingsUseCase {
    /**
     * 암호화 설정 초기화 (회원가입 시)
     * Server Key를 생성하고 암호화 설정을 저장합니다.
     *
     * @return SetupEncryptionResult (설정 + 평문 Server Key)
     * @throws EncryptionAlreadySetupException 이미 설정된 경우
     */
    suspend fun setup(memberId: MemberId, command: SetupEncryptionCommand): SetupEncryptionResult

    /**
     * 암호화 설정 조회 (로그인 시 DEK 복호화용)
     * Server Key를 복호화하여 함께 반환합니다.
     *
     * @return EncryptionSettingsWithServerKey
     * @throws EncryptionSettingsNotFoundException 설정이 없는 경우
     */
    suspend fun getSettings(memberId: MemberId): EncryptionSettingsWithServerKey

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
     * PIN 변경 시 암호화 키 업데이트
     * (새 Client KEK + 새 Server Key로 DEK 재암호화)
     *
     * @return UpdateEncryptionResult (설정 + 새 평문 Server Key)
     * @throws EncryptionSettingsNotFoundException 설정이 없는 경우
     */
    suspend fun updateEncryption(memberId: MemberId, command: UpdateEncryptionCommand): UpdateEncryptionResult

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
 *
 * encryptedDEK는 초기 설정 시 null일 수 있습니다.
 * 서버가 serverKey를 반환한 후, 클라이언트가 updateEncryption으로 설정합니다.
 */
data class SetupEncryptionCommand(
    val salt: String,
    val encryptedDEK: String?, // null 가능 - serverKey 반환 후 업데이트 필요
    val recoveryEncryptedDEK: String,
    val recoveryKeyHash: String,
)

/**
 * 암호화 키 업데이트 Command (비밀번호 변경 시)
 */
data class UpdateEncryptionCommand(
    val salt: String,
    val encryptedDEK: String,
    val rotateServerKey: Boolean,
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

/**
 * 암호화 설정 초기화 Result
 * Server Key를 생성하고 클라이언트에 전달합니다.
 */
data class SetupEncryptionResult(
    val settings: EncryptionSettings,
    val serverKey: String,  // Base64 인코딩된 평문 Server Key (클라이언트 전달용)
)

/**
 * 암호화 설정 조회 Result
 * Server Key를 복호화하여 함께 반환합니다.
 */
data class EncryptionSettingsWithServerKey(
    val salt: String,
    val encryptedDEK: String,
    val serverKey: String,  // Base64 인코딩된 평문 Server Key
)

/**
 * 암호화 키 업데이트 Result
 * 새 Server Key를 생성하여 반환합니다.
 */
data class UpdateEncryptionResult(
    val settings: EncryptionSettings,
    val serverKey: String,  // Base64 인코딩된 새 평문 Server Key
)
