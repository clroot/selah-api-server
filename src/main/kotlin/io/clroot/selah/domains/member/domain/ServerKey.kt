package io.clroot.selah.domains.member.domain

import io.clroot.selah.common.domain.AggregateRoot
import java.time.LocalDateTime

/**
 * Server Key Aggregate Root
 *
 * 6자리 PIN의 보안 취약점을 보완하기 위한 서버 측 키입니다.
 * Client KEK와 Server Key를 결합하여 Combined KEK를 생성하고,
 * 이를 통해 DEK를 암호화합니다.
 *
 * Server Key는 Master Key로 암호화되어 저장됩니다.
 * - encryptedServerKey: AES-256-GCM으로 암호화된 Server Key (Base64)
 * - iv: 암호화에 사용된 Initialization Vector (Base64)
 */
class ServerKey(
    override val id: ServerKeyId,
    val memberId: MemberId,
    encryptedServerKey: String,
    iv: String,
    version: Long?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) : AggregateRoot<ServerKeyId>(id, version, createdAt, updatedAt) {
    /**
     * Master Key로 암호화된 Server Key (Base64 인코딩)
     */
    var encryptedServerKey: String = encryptedServerKey
        private set

    /**
     * 암호화에 사용된 IV (Base64 인코딩)
     */
    var iv: String = iv
        private set

    /**
     * Server Key 재생성 시 암호화된 키와 IV 업데이트
     */
    fun updateServerKey(
        newEncryptedServerKey: String,
        newIv: String,
    ) {
        require(newEncryptedServerKey.isNotBlank()) { "Encrypted server key cannot be blank" }
        require(newIv.isNotBlank()) { "IV cannot be blank" }
        encryptedServerKey = newEncryptedServerKey
        iv = newIv
        touch()
    }

    companion object {
        fun create(
            memberId: MemberId,
            encryptedServerKey: String,
            iv: String,
        ): ServerKey {
            require(encryptedServerKey.isNotBlank()) { "Encrypted server key cannot be blank" }
            require(iv.isNotBlank()) { "IV cannot be blank" }

            val now = LocalDateTime.now()
            return ServerKey(
                id = ServerKeyId.new(),
                memberId = memberId,
                encryptedServerKey = encryptedServerKey,
                iv = iv,
                version = null,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
