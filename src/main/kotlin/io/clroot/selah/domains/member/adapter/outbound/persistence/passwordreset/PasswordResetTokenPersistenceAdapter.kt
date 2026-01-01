package io.clroot.selah.domains.member.adapter.outbound.persistence.passwordreset

import io.clroot.selah.common.util.HexSupport.hashSha256
import io.clroot.selah.common.util.HexSupport.toHexString
import io.clroot.selah.common.util.ULIDSupport
import io.clroot.selah.domains.member.application.port.outbound.PasswordResetTokenCreateResult
import io.clroot.selah.domains.member.application.port.outbound.PasswordResetTokenInfo
import io.clroot.selah.domains.member.application.port.outbound.PasswordResetTokenPort
import io.clroot.selah.domains.member.domain.MemberId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime

/**
 * 비밀번호 재설정 토큰 Persistence Adapter
 */
@Component
class PasswordResetTokenPersistenceAdapter(
    private val repository: PasswordResetTokenJpaRepository,
    @Value($$"${selah.password-reset.ttl:PT1H}")
    private val tokenTtl: Duration,
) : PasswordResetTokenPort {
    companion object {
        private const val TOKEN_LENGTH = 32
        private val SECURE_RANDOM = SecureRandom()
    }

    @Transactional
    override suspend fun create(memberId: MemberId): PasswordResetTokenCreateResult =
        withContext(Dispatchers.IO) {
            val now = LocalDateTime.now()
            val id = ULIDSupport.generateULID()

            // 랜덤 토큰 생성
            val rawToken = generateToken()
            val tokenHash = hashToken(rawToken)

            val entity =
                PasswordResetTokenEntity(
                    id = id,
                    tokenHash = tokenHash,
                    memberId = memberId.value,
                    expiresAt = now.plus(tokenTtl),
                    createdAt = now,
                )

            repository.save(entity)

            PasswordResetTokenCreateResult(
                info = entity.toInfo(),
                rawToken = rawToken,
            )
        }

    override suspend fun findValidByToken(token: String): PasswordResetTokenInfo? =
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

    override suspend fun invalidateAllByMemberId(memberId: MemberId) {
        withContext(Dispatchers.IO) {
            repository.deleteAllByMemberId(memberId.value)
        }
    }

    override suspend fun findLatestCreatedAtByMemberId(memberId: MemberId): LocalDateTime? =
        withContext(Dispatchers.IO) {
            repository.findTopByMemberIdOrderByCreatedAtDesc(memberId.value)?.createdAt
        }

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
    private fun hashToken(token: String): String = hashSha256(token)

    private fun PasswordResetTokenEntity.toInfo(): PasswordResetTokenInfo =
        PasswordResetTokenInfo(
            id = id,
            memberId = MemberId.from(memberId),
            expiresAt = expiresAt,
            usedAt = usedAt,
            createdAt = createdAt,
        )
}
