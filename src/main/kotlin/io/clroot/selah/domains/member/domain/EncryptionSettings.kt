package io.clroot.selah.domains.member.domain

import io.clroot.selah.common.domain.AggregateRoot
import java.time.LocalDateTime

/**
 * E2E 암호화 설정 Aggregate Root
 *
 * 클라이언트 측 암호화를 위한 Salt와 복구 키 해시를 저장합니다.
 * 암호화 키 자체는 서버에 저장되지 않으며, 클라이언트에서만 관리됩니다.
 */
class EncryptionSettings(
    override val id: EncryptionSettingsId,
    val memberId: MemberId,
    salt: String,
    recoveryKeyHash: String,
    isEnabled: Boolean,
    version: Long?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) : AggregateRoot<EncryptionSettingsId>(id, version, createdAt, updatedAt) {

    /**
     * 키 파생용 Salt (Base64 인코딩)
     */
    var salt: String = salt
        private set

    /**
     * 복구 키 해시 (검증용)
     */
    var recoveryKeyHash: String = recoveryKeyHash
        private set

    /**
     * 암호화 활성화 여부
     */
    var isEnabled: Boolean = isEnabled
        private set

    /**
     * Salt 업데이트
     */
    fun updateSalt(newSalt: String) {
        require(newSalt.isNotBlank()) { "Salt cannot be blank" }
        salt = newSalt
        touch()
    }

    /**
     * 복구 키 해시 업데이트
     */
    fun updateRecoveryKeyHash(newHash: String) {
        require(newHash.isNotBlank()) { "Recovery key hash cannot be blank" }
        recoveryKeyHash = newHash
        touch()
    }

    /**
     * 암호화 활성화
     */
    fun enable() {
        if (!isEnabled) {
            isEnabled = true
            touch()
        }
    }

    /**
     * 암호화 비활성화
     */
    fun disable() {
        if (isEnabled) {
            isEnabled = false
            touch()
        }
    }

    companion object {
        fun create(
            memberId: MemberId,
            salt: String,
            recoveryKeyHash: String,
        ): EncryptionSettings {
            require(salt.isNotBlank()) { "Salt cannot be blank" }
            require(recoveryKeyHash.isNotBlank()) { "Recovery key hash cannot be blank" }

            val now = LocalDateTime.now()
            return EncryptionSettings(
                id = EncryptionSettingsId.new(),
                memberId = memberId,
                salt = salt,
                recoveryKeyHash = recoveryKeyHash,
                isEnabled = true,
                version = null,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
