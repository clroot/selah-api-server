package io.clroot.selah.domains.member.adapter.outbound.encryption

import io.clroot.selah.domains.member.application.port.outbound.EncryptedServerKeyResult
import io.clroot.selah.domains.member.application.port.outbound.ServerKeyEncryptionPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Server Key 암호화/복호화 Adapter
 *
 * AES-256-GCM을 사용하여 Server Key를 암호화합니다.
 * Master Key는 환경변수 ENCRYPTION_MASTER_KEY에서 로드됩니다.
 */
@Component
class ServerKeyEncryptionAdapter(
    @Value($$"${selah.encryption.master-key}")
    private val masterKeyBase64: String,
) : ServerKeyEncryptionPort {
    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}

        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BYTES = 32 // 256 bits
        private const val IV_SIZE_BYTES = 12 // 96 bits (GCM 권장)
        private const val TAG_SIZE_BITS = 128 // GCM 태그 크기
    }

    private val masterKey: SecretKey by lazy {
        val keyBytes = Base64.getDecoder().decode(masterKeyBase64)
        require(keyBytes.size == KEY_SIZE_BYTES) {
            "Master key must be 256 bits (32 bytes), got ${keyBytes.size} bytes"
        }
        SecretKeySpec(keyBytes, "AES")
    }

    override fun generateAndEncryptServerKey(): EncryptedServerKeyResult {
        // 랜덤 Server Key 생성 (256 bits)
        val serverKeyBytes = ByteArray(KEY_SIZE_BYTES)
        SecureRandom().nextBytes(serverKeyBytes)

        // IV 생성
        val iv = ByteArray(IV_SIZE_BYTES)
        SecureRandom().nextBytes(iv)

        // 암호화
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, GCMParameterSpec(TAG_SIZE_BITS, iv))
        val encryptedBytes = cipher.doFinal(serverKeyBytes)

        val result =
            EncryptedServerKeyResult(
                encryptedServerKey = Base64.getEncoder().encodeToString(encryptedBytes),
                iv = Base64.getEncoder().encodeToString(iv),
                plainServerKey = Base64.getEncoder().encodeToString(serverKeyBytes),
            )

        logger.debug { "Generated and encrypted new server key" }
        return result
    }

    override fun decryptServerKey(
        encryptedServerKey: String,
        iv: String,
    ): String {
        val encryptedBytes = Base64.getDecoder().decode(encryptedServerKey)
        val ivBytes = Base64.getDecoder().decode(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(TAG_SIZE_BITS, ivBytes))
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        logger.debug { "Decrypted server key" }
        return Base64.getEncoder().encodeToString(decryptedBytes)
    }
}
