package io.clroot.selah.domains.member.adapter.outbound.persistence.emailverification

import io.clroot.selah.common.util.ULIDSupport
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenCreateResult
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenInfo
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenPort
import io.clroot.selah.domains.member.domain.MemberId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime

/**
 * 이메일 인증 토큰 Persistence Adapter
 */
@Component
class EmailVerificationTokenPersistenceAdapter(
    private val repository: EmailVerificationTokenJpaRepository,
    @Value("\${selah.email-verification.ttl:P1D}")
    private val tokenTtl: Duration,
) : EmailVerificationTokenPort {

    companion object {
        private const val TOKEN_LENGTH = 32
        private val SECURE_RANDOM = SecureRandom()
        private val HEX_CHARS = "0123456789abcdef".toCharArray()
    }

    @Transactional
    override suspend fun create(memberId: MemberId): EmailVerificationTokenCreateResult =
        withContext(Dispatchers.IO) {
            val now = LocalDateTime.now()
            val id = ULIDSupport.generateULID()

            // 랜덤 토큰 생성
            val rawToken = generateToken()
            val tokenHash = hashToken(rawToken)

            val entity = EmailVerificationTokenEntity(
                id = id,
                tokenHash = tokenHash,
                memberId = memberId.value,
                expiresAt = now.plus(tokenTtl),
                createdAt = now,
            )

            repository.save(entity)

            EmailVerificationTokenCreateResult(
                info = entity.toInfo(),
                rawToken = rawToken,
            )
        }

    override suspend fun findValidByToken(token: String): EmailVerificationTokenInfo? =
        withContext(Dispatchers.IO) {
            val tokenHash = hashToken(token)
            val entity = repository.findByTokenHash(tokenHash) ?: return@withContext null

            val info = entity.toInfo()
            if (!info.isValid()) {
                return@withContext null
            }

            info
        }

    @Transactional
    override suspend fun markAsUsed(id: String) {
        withContext(Dispatchers.IO) {
            val entity = repository.findByIdOrNull(id) ?: return@withContext
            entity.usedAt = LocalDateTime.now()
            repository.save(entity)
        }
    }

    @Transactional
    override suspend fun invalidateAllByMemberId(memberId: MemberId) {
        withContext(Dispatchers.IO) {
            repository.deleteAllByMemberId(memberId.value)
        }
    }

    override suspend fun findLatestCreatedAtByMemberId(memberId: MemberId): LocalDateTime? =
        withContext(Dispatchers.IO) {
            repository.findTopByMemberIdOrderByCreatedAtDesc(memberId.value)?.createdAt
        }

    @Transactional
    override suspend fun deleteExpiredTokens(): Int =
        withContext(Dispatchers.IO) {
            repository.deleteAllByExpiresAtBefore(LocalDateTime.now())
        }

    /**
     * 랜덤 토큰 생성 (hex 문자열)
     */
    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_LENGTH)
        SECURE_RANDOM.nextBytes(bytes)
        return bytes.toHexString()
    }

    /**
     * 토큰 해싱 (SHA-256)
     */
    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
        return hashBytes.toHexString()
    }

    private fun ByteArray.toHexString(): String {
        val result = StringBuilder(size * 2)
        for (byte in this) {
            val i = byte.toInt()
            result.append(HEX_CHARS[(i shr 4) and 0x0F])
            result.append(HEX_CHARS[i and 0x0F])
        }
        return result.toString()
    }

    private fun EmailVerificationTokenEntity.toInfo(): EmailVerificationTokenInfo =
        EmailVerificationTokenInfo(
            id = id,
            memberId = MemberId.from(memberId),
            expiresAt = expiresAt,
            usedAt = usedAt,
            createdAt = createdAt,
        )
}
