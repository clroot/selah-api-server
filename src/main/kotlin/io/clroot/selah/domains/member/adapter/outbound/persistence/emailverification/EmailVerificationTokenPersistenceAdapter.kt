package io.clroot.selah.domains.member.adapter.outbound.persistence.emailverification

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.selah.common.util.HexSupport.hashSha256
import io.clroot.selah.common.util.HexSupport.toHexString
import io.clroot.selah.common.util.ULIDSupport
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenCreateResult
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenInfo
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenPort
import io.clroot.selah.domains.member.domain.MemberId
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime

/**
 * 이메일 인증 토큰 Persistence Adapter (Reactive)
 */
@Component
class EmailVerificationTokenPersistenceAdapter(
    private val sessionFactory: Mutiny.SessionFactory,
    private val jpqlRenderContext: JpqlRenderContext,
    @Value("\${selah.email-verification.ttl:P1D}")
    private val tokenTtl: Duration,
) : EmailVerificationTokenPort {
    companion object {
        private const val TOKEN_LENGTH = 32
        private val SECURE_RANDOM = SecureRandom()
    }

    override suspend fun create(memberId: MemberId): EmailVerificationTokenCreateResult =
        sessionFactory
            .withTransaction { session, _ ->
                val now = LocalDateTime.now()
                val id = ULIDSupport.generateULID()

                val rawToken = generateToken()
                val tokenHash = hashToken(rawToken)

                val entity =
                    EmailVerificationTokenEntity(
                        id = id,
                        tokenHash = tokenHash,
                        memberId = memberId.value,
                        expiresAt = now.plus(tokenTtl),
                        createdAt = now,
                    )

                session.persist(entity).replaceWith(
                    EmailVerificationTokenCreateResult(
                        info = entity.toInfo(),
                        rawToken = rawToken,
                    ),
                )
            }.awaitSuspending()

    override suspend fun findValidByToken(token: String): EmailVerificationTokenInfo? =
        sessionFactory
            .withSession { session ->
                val tokenHash = hashToken(token)
                val now = LocalDateTime.now()
                session
                    .createQuery(
                        jpql {
                            select(entity(EmailVerificationTokenEntity::class))
                                .from(entity(EmailVerificationTokenEntity::class))
                                .where(
                                    and(
                                        path(EmailVerificationTokenEntity::tokenHash).eq(tokenHash),
                                        path(EmailVerificationTokenEntity::expiresAt).gt(now),
                                        path(EmailVerificationTokenEntity::usedAt).isNull(),
                                    ),
                                )
                        },
                        jpqlRenderContext,
                    ).singleResultOrNull
            }.awaitSuspending()
            ?.toInfo()

    override suspend fun markAsUsed(id: String) {
        sessionFactory
            .withTransaction { session, _ ->
                val query =
                    jpql {
                        update(entity(EmailVerificationTokenEntity::class))
                            .set(path(EmailVerificationTokenEntity::usedAt), LocalDateTime.now())
                            .where(path(EmailVerificationTokenEntity::id).eq(id))
                    }
                session.createMutationQuery(query, jpqlRenderContext).executeUpdate()
            }.awaitSuspending()
    }

    override suspend fun invalidateAllByMemberId(memberId: MemberId) {
        sessionFactory
            .withTransaction { session, _ ->
                val query =
                    jpql {
                        deleteFrom(entity(EmailVerificationTokenEntity::class))
                            .where(path(EmailVerificationTokenEntity::memberId).eq(memberId.value))
                    }
                session.createMutationQuery(query, jpqlRenderContext).executeUpdate()
            }.awaitSuspending()
    }

    override suspend fun findLatestCreatedAtByMemberId(memberId: MemberId): LocalDateTime? =
        sessionFactory
            .withSession { session ->
                session
                    .createQuery(
                        jpql {
                            select(path(EmailVerificationTokenEntity::createdAt))
                                .from(entity(EmailVerificationTokenEntity::class))
                                .where(path(EmailVerificationTokenEntity::memberId).eq(memberId.value))
                                .orderBy(path(EmailVerificationTokenEntity::createdAt).desc())
                        },
                        jpqlRenderContext,
                    ).setMaxResults(1)
                    .singleResultOrNull
            }.awaitSuspending()

    override suspend fun deleteExpiredTokens(): Int =
        sessionFactory
            .withTransaction { session, _ ->
                val query =
                    jpql {
                        deleteFrom(entity(EmailVerificationTokenEntity::class))
                            .where(path(EmailVerificationTokenEntity::expiresAt).lt(LocalDateTime.now()))
                    }
                session
                    .createMutationQuery(
                        query,
                        jpqlRenderContext,
                    ).executeUpdate()
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

    private fun EmailVerificationTokenEntity.toInfo(): EmailVerificationTokenInfo =
        EmailVerificationTokenInfo(
            id = id,
            memberId = MemberId.from(memberId),
            expiresAt = expiresAt,
            usedAt = usedAt,
            createdAt = createdAt,
        )
}
