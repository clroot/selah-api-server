package io.clroot.selah.domains.member.adapter.outbound.persistence.emailverification

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.hibernate.reactive.ReactiveSessionProvider
import io.clroot.selah.common.util.HexSupport.hashSha256
import io.clroot.selah.common.util.HexSupport.toHexString
import io.clroot.selah.common.util.ULIDSupport
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenCreateResult
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenInfo
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenPort
import io.clroot.selah.domains.member.domain.MemberId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime

@Component
class EmailVerificationTokenPersistenceAdapter(
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    @Value($$"${selah.email-verification.ttl:P1D}")
    private val tokenTtl: Duration,
) : EmailVerificationTokenPort {
    companion object {
        private const val TOKEN_LENGTH = 32
        private val SECURE_RANDOM = SecureRandom()
    }

    override suspend fun create(memberId: MemberId): EmailVerificationTokenCreateResult =
        sessions.write { session ->
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
        }

    override suspend fun findValidByToken(token: String): EmailVerificationTokenInfo? {
        val tokenHash = hashToken(token)
        val now = LocalDateTime.now()
        return sessions.read { session ->
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
                .map { it?.toInfo() }
        }
    }

    override suspend fun markAsUsed(id: String) {
        sessions.write { session ->
            session
                .createMutationQuery(
                    jpql {
                        update(entity(EmailVerificationTokenEntity::class))
                            .set(path(EmailVerificationTokenEntity::usedAt), LocalDateTime.now())
                            .where(path(EmailVerificationTokenEntity::id).eq(id))
                    },
                    jpqlRenderContext,
                ).executeUpdate()
        }
    }

    override suspend fun invalidateAllByMemberId(memberId: MemberId) {
        sessions.write { session ->
            session
                .createMutationQuery(
                    jpql {
                        deleteFrom(entity(EmailVerificationTokenEntity::class))
                            .where(path(EmailVerificationTokenEntity::memberId).eq(memberId.value))
                    },
                    jpqlRenderContext,
                ).executeUpdate()
        }
    }

    override suspend fun findLatestCreatedAtByMemberId(memberId: MemberId): LocalDateTime? =
        sessions.read { session ->
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
        }

    override suspend fun deleteExpiredTokens(): Int =
        sessions.write { session ->
            session
                .createMutationQuery(
                    jpql {
                        deleteFrom(entity(EmailVerificationTokenEntity::class))
                            .where(path(EmailVerificationTokenEntity::expiresAt).lt(LocalDateTime.now()))
                    },
                    jpqlRenderContext,
                ).executeUpdate()
                .map { it ?: 0 }
        }

    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_LENGTH)
        SECURE_RANDOM.nextBytes(bytes)
        return bytes.toHexString()
    }

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
