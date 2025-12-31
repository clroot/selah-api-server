package io.clroot.selah.domains.member.adapter.outbound.persistence.encryption

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDateTime

/**
 * EncryptionSettings JPA Entity
 *
 * Domain의 EncryptionSettings와 분리된 Persistence Layer Entity입니다.
 *
 * DEK/KEK 구조:
 * - salt: KEK 파생용
 * - encryptedDek: KEK로 암호화된 DEK
 * - recoveryEncryptedDek: 복구 키로 암호화된 DEK
 * - recoveryKeyHash: 복구 키 검증용 해시
 */
@Entity
@Table(
    name = "encryption_settings",
    indexes = [
        Index(name = "idx_encryption_settings_member_id", columnList = "member_id", unique = true),
    ],
)
class EncryptionSettingsEntity(
    @Id
    @Column(name = "id", length = 26)
    val id: String,

    @Column(name = "member_id", nullable = false, unique = true, length = 26)
    val memberId: String,

    @Column(name = "salt", nullable = false, columnDefinition = "TEXT")
    var salt: String,

    @Column(name = "encrypted_dek", nullable = false, length = 512)
    var encryptedDek: String,

    @Column(name = "recovery_encrypted_dek", nullable = false, length = 512)
    var recoveryEncryptedDek: String,

    @Column(name = "recovery_key_hash", nullable = false)
    var recoveryKeyHash: String,

    @Version
    @Column(name = "version")
    var version: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
)
