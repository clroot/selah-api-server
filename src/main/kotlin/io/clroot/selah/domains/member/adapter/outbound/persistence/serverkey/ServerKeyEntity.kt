package io.clroot.selah.domains.member.adapter.outbound.persistence.serverkey

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDateTime

/**
 * ServerKey JPA Entity
 *
 * Domain의 ServerKey와 분리된 Persistence Layer Entity입니다.
 *
 * Server Key는 6자리 PIN의 보안 취약점을 보완합니다.
 * Client KEK와 결합하여 Combined KEK를 생성하고, 이를 통해 DEK를 암호화합니다.
 *
 * - encryptedServerKey: Master Key로 암호화된 Server Key (Base64)
 * - iv: 암호화에 사용된 IV (Base64)
 */
@Entity
@Table(
    name = "server_keys",
    indexes = [
        Index(name = "idx_server_keys_member_id", columnList = "member_id", unique = true),
    ],
)
class ServerKeyEntity(
    @Id
    @Column(name = "id", length = 26)
    val id: String,

    @Column(name = "member_id", nullable = false, unique = true, length = 26)
    val memberId: String,

    @Column(name = "encrypted_server_key", nullable = false, length = 512)
    var encryptedServerKey: String,

    @Column(name = "iv", nullable = false, length = 64)
    var iv: String,

    @Version
    @Column(name = "version")
    var version: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
)
