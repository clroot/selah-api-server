package io.clroot.selah.common.util

import java.security.SecureRandom
import java.time.Instant
import java.util.*

object ULIDSupport {
    private const val ULID_LENGTH = 26
    private const val ULID_BYTE_LENGTH = 16
    private val ULID_PATTERN = Regex("^[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{26}$")
    private const val BASE32_CHARS = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private val secureRandom = SecureRandom()

    /**
     * 새로운 ULID 문자열 생성
     */
    fun generateULID(): String {
        val timestamp = Instant.now().toEpochMilli()
        val timestampPart = Base32Codec.encode(timestamp, 10)

        // 80비트(10바이트) 랜덤 생성
        val randomBytes = ByteArray(10)
        secureRandom.nextBytes(randomBytes)

        // 16자리 랜덤 부분을 8자리씩 두 부분으로 나누어 처리
        var randomValue1 = 0L
        for (i in 0..4) { // 첫 5바이트 = 40비트 = 8자리 Base32
            randomValue1 = (randomValue1 shl 8) or (randomBytes[i].toInt() and 0xFF).toLong()
        }
        val randomPart1 = Base32Codec.encode(randomValue1, 8)

        var randomValue2 = 0L
        for (i in 5..9) { // 나머지 5바이트 = 40비트 = 8자리 Base32
            randomValue2 = (randomValue2 shl 8) or (randomBytes[i].toInt() and 0xFF).toLong()
        }
        val randomPart2 = Base32Codec.encode(randomValue2, 8)

        return timestampPart + randomPart1 + randomPart2
    }

    /**
     * ULID 문자열 유효성 검증
     */
    fun isValidULID(ulid: String): Boolean {
        if (ulid.length != ULID_LENGTH) return false
        return ULID_PATTERN.matches(ulid)
    }

    /**
     * ULID를 바이너리로 변환
     */
    fun ulidToBytes(ulid: String): ByteArray {
        require(isValidULID(ulid)) { "Invalid ULID format: $ulid" }
        return UUIDBridge.ulidToBytes(ulid)
    }

    /**
     * 바이너리를 ULID로 변환
     */
    fun bytesToULID(bytes: ByteArray): String {
        require(bytes.size == ULID_BYTE_LENGTH) {
            "Binary data must be $ULID_BYTE_LENGTH bytes, got ${bytes.size}"
        }
        return UUIDBridge.bytesToULID(bytes)
    }

    /**
     * UUID를 ULID 문자열로 변환
     */
    fun uuidToULID(uuid: UUID): String = UUIDBridge.uuidToULID(uuid)

    /**
     * ULID 문자열을 UUID로 변환
     */
    fun ulidToUUID(ulid: String): UUID = UUIDBridge.ulidToUUID(ulid)

    /**
     * Crockford Base32 인코딩/디코딩
     */
    private object Base32Codec {
        fun decode(str: String): Long {
            var result = 0L
            for (c in str) {
                val value = BASE32_CHARS.indexOf(c)
                if (value == -1) throw IllegalArgumentException("Invalid base32 character: $c")
                result = (result shl 5) or value.toLong()
            }
            return result
        }

        fun encode(
            value: Long,
            length: Int,
        ): String {
            val result = StringBuilder()
            var remaining = value

            repeat(length) {
                val index = (remaining and 0x1F).toInt()
                result.append(BASE32_CHARS[index])
                remaining = remaining shr 5
            }

            return result.reverse().toString()
        }
    }

    /**
     * ULID와 바이너리 변환을 위한 UUID 브리지
     */
    private object UUIDBridge {
        fun ulidToBytes(ulid: String): ByteArray {
            val uuid = ulidToUUID(ulid)
            val bytes = ByteArray(16)

            val mostSigBits = uuid.mostSignificantBits
            val leastSigBits = uuid.leastSignificantBits

            for (i in 0..7) {
                bytes[i] = (mostSigBits shr (8 * (7 - i))).toByte()
            }
            for (i in 0..7) {
                bytes[i + 8] = (leastSigBits shr (8 * (7 - i))).toByte()
            }

            return bytes
        }

        fun bytesToULID(bytes: ByteArray): String {
            var mostSigBits = 0L
            var leastSigBits = 0L

            for (i in 0..7) {
                mostSigBits = (mostSigBits shl 8) or (bytes[i].toLong() and 0xFF)
            }
            for (i in 0..7) {
                leastSigBits = (leastSigBits shl 8) or (bytes[i + 8].toLong() and 0xFF)
            }

            val uuid = UUID(mostSigBits, leastSigBits)
            return uuidToULID(uuid)
        }

        fun ulidToUUID(ulid: String): UUID {
            val timestamp = Base32Codec.decode(ulid.substring(0, 10)) // 48비트
            val random1 = Base32Codec.decode(ulid.substring(10, 18)) // 40비트
            val random2 = Base32Codec.decode(ulid.substring(18, 26)) // 40비트

            val mostSigBits = (timestamp shl 16) or (random1 shr 24)
            val leastSigBits = ((random1 and 0xFFFFFF) shl 40) or random2

            return UUID(mostSigBits, leastSigBits)
        }

        fun uuidToULID(uuid: UUID): String {
            val mostSigBits = uuid.mostSignificantBits
            val leastSigBits = uuid.leastSignificantBits

            val timestamp = mostSigBits shr 16
            val random1 = ((mostSigBits and 0xFFFF) shl 24) or (leastSigBits shr 40)
            val random2 = leastSigBits and 0xFFFFFFFFFF

            val timestampStr = Base32Codec.encode(timestamp, 10)
            val randomStr1 = Base32Codec.encode(random1, 8)
            val randomStr2 = Base32Codec.encode(random2, 8)

            return timestampStr + randomStr1 + randomStr2
        }
    }
}
