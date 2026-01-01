package io.clroot.selah.domains.member.application.port.outbound

/**
 * Server Key 암호화/복호화 Port
 *
 * Master Key를 사용하여 Server Key를 암호화하고 복호화합니다.
 * Master Key는 환경변수에서 로드됩니다.
 */
interface ServerKeyEncryptionPort {
    /**
     * 새 Server Key 생성 및 암호화
     *
     * @return Pair<encryptedServerKey, iv> (둘 다 Base64 인코딩)
     */
    fun generateAndEncryptServerKey(): EncryptedServerKeyResult

    /**
     * 암호화된 Server Key 복호화
     *
     * @param encryptedServerKey Base64 인코딩된 암호화된 Server Key
     * @param iv Base64 인코딩된 IV
     * @return Base64 인코딩된 평문 Server Key
     */
    fun decryptServerKey(
        encryptedServerKey: String,
        iv: String,
    ): String
}

/**
 * 암호화된 Server Key 결과
 */
data class EncryptedServerKeyResult(
    val encryptedServerKey: String, // Base64 인코딩
    val iv: String, // Base64 인코딩
    val plainServerKey: String, // Base64 인코딩된 평문 Server Key (클라이언트에 전달용)
)
