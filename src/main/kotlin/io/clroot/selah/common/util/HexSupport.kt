package io.clroot.selah.common.util

import java.security.MessageDigest

/**
 * Hex 인코딩 관련 유틸리티
 */
object HexSupport {

    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    /**
     * ByteArray를 hex 문자열로 변환
     */
    fun ByteArray.toHexString(): String {
        val result = StringBuilder(size * 2)
        for (byte in this) {
            val i = byte.toInt()
            result.append(HEX_CHARS[(i shr 4) and 0x0F])
            result.append(HEX_CHARS[i and 0x0F])
        }
        return result.toString()
    }

    /**
     * 문자열을 SHA-256으로 해싱하여 hex 문자열로 반환
     */
    fun hashSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.toHexString()
    }
}
