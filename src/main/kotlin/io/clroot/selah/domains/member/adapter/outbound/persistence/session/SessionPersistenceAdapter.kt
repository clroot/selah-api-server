package io.clroot.selah.domains.member.adapter.outbound.persistence.session

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.selah.common.reactive.ReactiveSessionProvider
import io.clroot.selah.domains.member.application.port.outbound.SessionInfo
import io.clroot.selah.domains.member.application.port.outbound.SessionPort
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.smallrye.mutiny.Uni
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@Component
class SessionPersistenceAdapter(
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    @Value($$"${selah.session.ttl:P7D}")
    private val sessionTtl: Duration,
    @Value($$"${selah.session.extend-threshold:P1D}")
    private val extendThreshold: Duration,
) : SessionPort {
    override suspend fun create(
        memberId: MemberId,
        role: Member.Role,
        userAgent: String?,
        ipAddress: String?,
    ): SessionInfo =
        sessions.write { session ->
            val now = LocalDateTime.now()
            val token = UUID.randomUUID().toString()
            val expiresAt = now.plus(sessionTtl)

            val entity =
                SessionEntity(
                    token = token,
                    memberId = memberId.value,
                    role = role,
                    userAgent = userAgent?.take(500),
                    createdIp = ipAddress?.take(45),
                    lastAccessedIp = ipAddress?.take(45),
                    expiresAt = expiresAt,
                    createdAt = now,
                )

            session.persist(entity).replaceWith(entity.toSessionInfo())
        }

    override suspend fun findByToken(token: String): SessionInfo? =
        sessions.read { session ->
            session
                .createQuery(
                    jpql {
                        select(entity(SessionEntity::class))
                            .from(entity(SessionEntity::class))
                            .where(path(SessionEntity::token).eq(token))
                    },
                    jpqlRenderContext,
                ).singleResultOrNull
                .map { it?.toSessionInfo() }
        }

    override suspend fun delete(token: String) {
        sessions.write { session ->
            session
                .createMutationQuery(
                    jpql {
                        deleteFrom(entity(SessionEntity::class))
                            .where(path(SessionEntity::token).eq(token))
                    },
                    jpqlRenderContext,
                ).executeUpdate()
        }
    }

    override suspend fun deleteAllByMemberId(memberId: MemberId) {
        sessions.write { session ->
            session
                .createMutationQuery(
                    jpql {
                        deleteFrom(entity(SessionEntity::class))
                            .where(path(SessionEntity::memberId).eq(memberId.value))
                    },
                    jpqlRenderContext,
                ).executeUpdate()
        }
    }

    override suspend fun extendExpiry(
        token: String,
        ipAddress: String?,
    ) {
        sessions.write { session ->
            session
                .createQuery(
                    jpql {
                        select(entity(SessionEntity::class))
                            .from(entity(SessionEntity::class))
                            .where(path(SessionEntity::token).eq(token))
                    },
                    jpqlRenderContext,
                ).singleResultOrNull
                .chain { entity: SessionEntity? ->
                    if (entity == null) {
                        return@chain Uni.createFrom().nullItem<SessionEntity>()
                    }
                    val now = LocalDateTime.now()
                    val remainingTime = Duration.between(now, entity.expiresAt)

                    entity.lastAccessedIp = ipAddress?.take(45)

                    if (remainingTime <= extendThreshold) {
                        entity.expiresAt = now.plus(sessionTtl)
                    }
                    session.merge(entity)
                }
        }
    }

    override suspend fun deleteExpiredSessions(): Int =
        sessions.write { session ->
            session
                .createMutationQuery(
                    jpql {
                        deleteFrom(entity(SessionEntity::class))
                            .where(path(SessionEntity::expiresAt).lt(LocalDateTime.now()))
                    },
                    jpqlRenderContext,
                ).executeUpdate()
                .map { it ?: 0 }
        }

    private fun SessionEntity.toSessionInfo(): SessionInfo =
        SessionInfo(
            token = token,
            memberId = MemberId.from(memberId),
            role = role,
            userAgent = userAgent,
            createdIp = createdIp,
            lastAccessedIp = lastAccessedIp,
            expiresAt = expiresAt,
            createdAt = createdAt,
        )
}
