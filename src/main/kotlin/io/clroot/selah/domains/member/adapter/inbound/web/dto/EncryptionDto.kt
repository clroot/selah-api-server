package io.clroot.selah.domains.member.adapter.inbound.web.dto

import io.clroot.selah.domains.member.application.port.inbound.EncryptionSettingsWithServerKey
import io.clroot.selah.domains.member.application.port.inbound.RecoverySettingsResult
import io.clroot.selah.domains.member.application.port.inbound.SetupEncryptionResult
import io.clroot.selah.domains.member.application.port.inbound.UpdateEncryptionResult
import io.clroot.selah.domains.member.domain.EncryptionSettings
import java.time.LocalDateTime

// === Request DTOs ===

/**
 * 암호화 설정 초기화 요청 (회원가입 시)
 *
 * encryptedDEK는 초기 설정 시 비어있을 수 있습니다.
 * 서버가 serverKey를 반환한 후, 클라이언트가 updateEncryption으로 encryptedDEK를 업데이트합니다.
 */
data class SetupEncryptionRequest(
    val salt: String,
    val encryptedDEK: String?, // null 허용 - serverKey 반환 후 업데이트
    val recoveryEncryptedDEK: String,
    val recoveryKeyHash: String,
)

/**
 * 암호화 키 업데이트 요청 (비밀번호 변경 시)
 */
data class UpdateEncryptionRequest(
    val salt: String,
    val encryptedDEK: String,
    val rotateServerKey: Boolean = false,
)

/**
 * 복구 키 재생성 요청
 */
data class UpdateRecoveryKeyRequest(
    val recoveryEncryptedDEK: String,
    val recoveryKeyHash: String,
)

/**
 * 복구 키 검증 요청
 */
data class VerifyRecoveryKeyRequest(
    val recoveryKeyHash: String,
)

// === Response DTOs ===

/**
 * 암호화 설정 응답 (로그인 시 DEK 복호화용)
 */
data class EncryptionSettingsResponse(
    val salt: String,
    val encryptedDEK: String,
    val serverKey: String,
)

/**
 * 암호화 설정 초기화 응답
 */
data class SetupEncryptionResponse(
    val serverKey: String,
    val createdAt: LocalDateTime,
)

/**
 * 복구용 설정 응답 (비밀번호 분실 시 DEK 복구용)
 */
data class RecoverySettingsResponse(
    val recoveryEncryptedDEK: String,
    val recoveryKeyHash: String,
)

/**
 * 복구 키 검증 응답
 */
data class VerifyRecoveryKeyResponse(
    val valid: Boolean,
)

/**
 * 암호화 키 업데이트 응답
 */
data class UpdateEncryptionResponse(
    val serverKey: String,
    val updatedAt: LocalDateTime,
)

/**
 * 복구 키 재생성 응답
 */
data class UpdateRecoveryKeyResponse(
    val updatedAt: LocalDateTime,
)

// === Extension Functions ===

fun EncryptionSettingsWithServerKey.toResponse(): EncryptionSettingsResponse = EncryptionSettingsResponse(
    salt = salt,
    encryptedDEK = encryptedDEK,
    serverKey = serverKey,
)

fun SetupEncryptionResult.toResponse(): SetupEncryptionResponse = SetupEncryptionResponse(
    serverKey = serverKey,
    createdAt = settings.createdAt,
)

fun RecoverySettingsResult.toResponse(): RecoverySettingsResponse = RecoverySettingsResponse(
    recoveryEncryptedDEK = recoveryEncryptedDEK,
    recoveryKeyHash = recoveryKeyHash,
)

fun UpdateEncryptionResult.toResponse(): UpdateEncryptionResponse = UpdateEncryptionResponse(
    serverKey = serverKey,
    updatedAt = settings.updatedAt,
)

fun EncryptionSettings.toUpdateRecoveryKeyResponse(): UpdateRecoveryKeyResponse = UpdateRecoveryKeyResponse(
    updatedAt = updatedAt,
)
