package io.clroot.selah.domains.member.adapter.outbound.persistence.session

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import io.clroot.hibernate.reactive.ReactiveSessionProvider
import io.clroot.selah.domains.member.application.port.outbound.SessionInfo
import io.clroot.selah.domains.member.application.port.outbound.SessionPort
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

/**
 * Session Persistence Adapter
 *
 * 단순 CRUD는 CoroutineCrudRepository를 사용하고,
 * 삭제 개수 반환이 필요한 deleteExpiredBefore는 JDSL을 사용합니다.
 */
@Component
class SessionPersistenceAdapter(
    private val repository: SessionEntityRepository,
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    @Value($$"${selah.session.ttl:P7D}")
    private val sessionTtl: Duration,
) : SessionPort {
    override suspend fun create(
        memberId: MemberId,
        role: Member.Role,
        userAgent: String?,
        ipAddress: String?,
    ): SessionInfo {
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

        val saved = repository.save(entity)
        return saved.toSessionInfo()
    }

    override suspend fun findByToken(token: String): SessionInfo? =
        repository.findByToken(token)?.toSessionInfo()

    override suspend fun update(sessionInfo: SessionInfo): SessionInfo {
        val entity =
            SessionEntity(
                token = sessionInfo.token,
                memberId = sessionInfo.memberId.value,
                role = sessionInfo.role,
                userAgent = sessionInfo.userAgent,
                createdIp = sessionInfo.createdIp,
                lastAccessedIp = sessionInfo.lastAccessedIp,
                expiresAt = sessionInfo.expiresAt,
                createdAt = sessionInfo.createdAt,
            )
        val saved = repository.save(entity)
        return saved.toSessionInfo()
    }

    override suspend fun delete(token: String) {
        repository.deleteByToken(token)
    }

    override suspend fun deleteAllByMemberId(memberId: MemberId) {
        repository.deleteAllByMemberId(memberId.value)
    }

    override suspend fun deleteExpiredBefore(before: LocalDateTime): Long =
        sessions.write { session ->
            session
                .createMutationQuery(
                    jpql {
                        deleteFrom(entity(SessionEntity::class))
                            .where(path(SessionEntity::expiresAt).lessThan(before))
                    },
                    jpqlRenderContext,
                ).executeUpdate()
                .map { (it ?: 0).toLong() }
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
