package io.clroot.selah.domains.member.adapter.inbound.web.dto

import io.clroot.selah.domains.member.domain.EncryptionSettings
import java.time.LocalDateTime

// === Request DTOs ===

/**
 * 암호화 설정 초기화 요청
 */
data class SetupEncryptionRequest(
    val salt: String,
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
 * 암호화 설정 응답
 */
data class EncryptionSettingsResponse(
    val salt: String,
    val isEnabled: Boolean,
    val createdAt: LocalDateTime,
)

/**
 * 암호화 설정 초기화 응답
 */
data class SetupEncryptionResponse(
    val isEnabled: Boolean,
    val createdAt: LocalDateTime,
)

/**
 * 복구 키 검증 응답
 */
data class VerifyRecoveryKeyResponse(
    val valid: Boolean,
)

// === Extension Functions ===

fun EncryptionSettings.toResponse(): EncryptionSettingsResponse = EncryptionSettingsResponse(
    salt = salt,
    isEnabled = isEnabled,
    createdAt = createdAt,
)

fun EncryptionSettings.toSetupResponse(): SetupEncryptionResponse = SetupEncryptionResponse(
    isEnabled = isEnabled,
    createdAt = createdAt,
)
