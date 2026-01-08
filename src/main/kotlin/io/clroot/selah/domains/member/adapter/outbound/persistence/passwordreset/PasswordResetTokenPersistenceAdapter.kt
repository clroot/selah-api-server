package io.clroot.selah.domains.member.adapter.outbound.persistence.passwordreset

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.selah.common.util.HexSupport.hashSha256
import io.clroot.selah.common.util.HexSupport.toHexString
import io.clroot.selah.common.util.ULIDSupport
import io.clroot.selah.domains.member.application.port.outbound.PasswordResetTokenCreateResult
import io.clroot.selah.domains.member.application.port.outbound.PasswordResetTokenInfo
import io.clroot.selah.domains.member.application.port.outbound.PasswordResetTokenPort
import io.clroot.selah.domains.member.domain.MemberId
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny
import org.springframework.beans.factory.annotation.Value
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
    private val sessionFactory: Mutiny.SessionFactory,
    private val jpqlRenderContext: JpqlRenderContext,
    @Value($$"${selah.password-reset.ttl:PT1H}")
    private val tokenTtl: Duration,
) : PasswordResetTokenPort {
    companion object {
        private const val TOKEN_LENGTH = 32
        private val SECURE_RANDOM = SecureRandom()
    }

    @Transactional
    override suspend fun create(memberId: MemberId): PasswordResetTokenCreateResult =
        sessionFactory
            .withTransaction { session ->
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

                session.persist(entity).replaceWith(
                    PasswordResetTokenCreateResult(
                        info = entity.toInfo(),
                        rawToken = rawToken,
                    ),
                )
            }.awaitSuspending()

    override suspend fun findValidByToken(token: String): PasswordResetTokenInfo? =
        sessionFactory
            .withSession { session ->
                val tokenHash = hashToken(token)
                val query =
                    jpql {
                        select(entity(PasswordResetTokenEntity::class))
                            .from(entity(PasswordResetTokenEntity::class))
                            .where(path(PasswordResetTokenEntity::tokenHash).eq(tokenHash))
                    }
                session.createQuery(query, jpqlRenderContext).singleResultOrNull
            }.awaitSuspending()
            ?.toInfo()
            ?.let {
                if (it.isValid()) it else null
            }

    @Transactional
    override suspend fun markAsUsed(id: String) {
        sessionFactory
            .withTransaction { session ->
                val query =
                    jpql {
                        update(entity(PasswordResetTokenEntity::class))
                            .set(path(PasswordResetTokenEntity::usedAt), LocalDateTime.now())
                            .where(path(PasswordResetTokenEntity::id).eq(id))
                    }
                session.createMutationQuery(query, jpqlRenderContext).executeUpdate()
            }.awaitSuspending()
    }

    override suspend fun invalidateAllByMemberId(memberId: MemberId) {
        sessionFactory
            .withTransaction { session ->
                val query =
                    jpql {
                        deleteFrom(entity(PasswordResetTokenEntity::class))
                            .where(path(PasswordResetTokenEntity::memberId).eq(memberId.value))
                    }
                session.createMutationQuery(query, jpqlRenderContext).executeUpdate()
            }.awaitSuspending()
    }

    override suspend fun findLatestCreatedAtByMemberId(memberId: MemberId): LocalDateTime? =
        sessionFactory
            .withSession { session ->
                val query =
                    jpql {
                        select(path(PasswordResetTokenEntity::createdAt))
                            .from(entity(PasswordResetTokenEntity::class))
                            .where(path(PasswordResetTokenEntity::memberId).eq(memberId.value))
                            .orderBy(path(PasswordResetTokenEntity::createdAt).desc())
                    }
                session.createQuery(query, jpqlRenderContext).resultList
            }.awaitSuspending()
            ?.firstOrNull()

    override suspend fun deleteExpiredTokens(): Int =
        sessionFactory
            .withTransaction { session ->
                val query =
                    jpql {
                        deleteFrom(entity(PasswordResetTokenEntity::class))
                            .where(path(PasswordResetTokenEntity::expiresAt).lt(LocalDateTime.now()))
                    }
                session.createMutationQuery(query, jpqlRenderContext).executeUpdate()
            }.awaitSuspending()

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
