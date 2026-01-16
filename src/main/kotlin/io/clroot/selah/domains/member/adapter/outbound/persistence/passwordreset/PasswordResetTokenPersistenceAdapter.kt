package io.clroot.selah.domains.member.adapter.outbound.persistence.passwordreset

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.hibernate.reactive.ReactiveSessionProvider
import io.clroot.selah.common.util.DateTimeSupport
import io.clroot.selah.common.util.HexSupport.hashSha256
import io.clroot.selah.common.util.HexSupport.toHexString
import io.clroot.selah.common.util.ULIDSupport
import io.clroot.selah.domains.member.application.port.outbound.PasswordResetTokenCreateResult
import io.clroot.selah.domains.member.application.port.outbound.PasswordResetTokenInfo
import io.clroot.selah.domains.member.application.port.outbound.PasswordResetTokenPort
import io.clroot.selah.domains.member.domain.MemberId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime

@Component
class PasswordResetTokenPersistenceAdapter(
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    @Value($$"${selah.password-reset.ttl:PT1H}")
    private val tokenTtl: Duration,
) : PasswordResetTokenPort {
    companion object {
        private const val TOKEN_LENGTH = 32
        private val SECURE_RANDOM = SecureRandom()
    }

    override suspend fun create(memberId: MemberId): PasswordResetTokenCreateResult =
        sessions.write { session ->
            val now = DateTimeSupport.now()
            val id = ULIDSupport.generateULID()
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
        }

    override suspend fun findValidByToken(token: String): PasswordResetTokenInfo? {
        val tokenHash = hashToken(token)
        val now = LocalDateTime.now()
        return sessions.read { session ->
            session
                .createQuery(
                    jpql {
                        select(entity(PasswordResetTokenEntity::class))
                            .from(entity(PasswordResetTokenEntity::class))
                            .where(
                                and(
                                    path(PasswordResetTokenEntity::tokenHash).eq(tokenHash),
                                    path(PasswordResetTokenEntity::expiresAt).gt(now),
                                    path(PasswordResetTokenEntity::usedAt).isNull(),
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
                        update(entity(PasswordResetTokenEntity::class))
                            .set(path(PasswordResetTokenEntity::usedAt), LocalDateTime.now())
                            .where(path(PasswordResetTokenEntity::id).eq(id))
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
                        deleteFrom(entity(PasswordResetTokenEntity::class))
                            .where(path(PasswordResetTokenEntity::memberId).eq(memberId.value))
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
                        select(path(PasswordResetTokenEntity::createdAt))
                            .from(entity(PasswordResetTokenEntity::class))
                            .where(path(PasswordResetTokenEntity::memberId).eq(memberId.value))
                            .orderBy(path(PasswordResetTokenEntity::createdAt).desc())
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
                        deleteFrom(entity(PasswordResetTokenEntity::class))
                            .where(path(PasswordResetTokenEntity::expiresAt).lt(LocalDateTime.now()))
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

    private fun PasswordResetTokenEntity.toInfo(): PasswordResetTokenInfo =
        PasswordResetTokenInfo(
            id = id,
            memberId = MemberId.from(memberId),
            expiresAt = expiresAt,
            usedAt = usedAt,
            createdAt = createdAt,
        )
}
