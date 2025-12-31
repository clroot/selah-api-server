package io.clroot.selah.domains.member.domain

import io.clroot.selah.common.domain.AggregateRoot
import java.time.LocalDateTime

/**
 * E2E 암호화 설정 Aggregate Root
 *
 * DEK/KEK 구조:
 * - DEK (Data Encryption Key): 실제 데이터 암호화 키, 클라이언트에서 랜덤 생성
 * - KEK (Key Encryption Key): 로그인 비밀번호에서 파생, DEK 암호화에 사용
 * - Recovery Key: DEK 복구용 키, 회원가입 시 1회만 표시
 *
 * 서버에는 암호화된 DEK만 저장되며, 평문 DEK와 KEK는 클라이언트에서만 관리됩니다.
 */
class EncryptionSettings(
    override val id: EncryptionSettingsId,
    val memberId: MemberId,
    salt: String,
    encryptedDEK: String,
    recoveryEncryptedDEK: String,
    recoveryKeyHash: String,
    version: Long?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) : AggregateRoot<EncryptionSettingsId>(id, version, createdAt, updatedAt) {

    /**
     * KEK 파생용 Salt (Base64 인코딩)
     */
    var salt: String = salt
        private set

    /**
     * KEK로 암호화된 DEK (Base64 인코딩)
     */
    var encryptedDEK: String = encryptedDEK
        private set

    /**
     * 복구 키로 암호화된 DEK (Base64 인코딩)
     */
    var recoveryEncryptedDEK: String = recoveryEncryptedDEK
        private set

    /**
     * 복구 키 해시 (검증용)
     */
    var recoveryKeyHash: String = recoveryKeyHash
        private set

    /**
     * 비밀번호 변경 시 Salt와 encryptedDEK 업데이트
     * (DEK 자체는 변경되지 않음, 새 KEK로 재암호화된 DEK)
     */
    fun updateEncryption(newSalt: String, newEncryptedDEK: String) {
        require(newSalt.isNotBlank()) { "Salt cannot be blank" }
        require(newEncryptedDEK.isNotBlank()) { "Encrypted DEK cannot be blank" }
        salt = newSalt
        encryptedDEK = newEncryptedDEK
        touch()
    }

    /**
     * 복구 키 재생성 시 recoveryEncryptedDEK, recoveryKeyHash 업데이트
     */
    fun updateRecoveryKey(newRecoveryEncryptedDEK: String, newRecoveryKeyHash: String) {
        require(newRecoveryEncryptedDEK.isNotBlank()) { "Recovery encrypted DEK cannot be blank" }
        require(newRecoveryKeyHash.isNotBlank()) { "Recovery key hash cannot be blank" }
        recoveryEncryptedDEK = newRecoveryEncryptedDEK
        recoveryKeyHash = newRecoveryKeyHash
        touch()
    }

    companion object {
        fun create(
            memberId: MemberId,
            salt: String,
            encryptedDEK: String,
            recoveryEncryptedDEK: String,
            recoveryKeyHash: String,
        ): EncryptionSettings {
            require(salt.isNotBlank()) { "Salt cannot be blank" }
            require(encryptedDEK.isNotBlank()) { "Encrypted DEK cannot be blank" }
            require(recoveryEncryptedDEK.isNotBlank()) { "Recovery encrypted DEK cannot be blank" }
            require(recoveryKeyHash.isNotBlank()) { "Recovery key hash cannot be blank" }

            val now = LocalDateTime.now()
            return EncryptionSettings(
                id = EncryptionSettingsId.new(),
                memberId = memberId,
                salt = salt,
                encryptedDEK = encryptedDEK,
                recoveryEncryptedDEK = recoveryEncryptedDEK,
                recoveryKeyHash = recoveryKeyHash,
                version = null,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
