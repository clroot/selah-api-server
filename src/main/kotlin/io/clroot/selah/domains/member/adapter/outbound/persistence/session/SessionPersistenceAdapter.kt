package io.clroot.selah.domains.member.adapter.outbound.persistence.session

import io.clroot.selah.domains.member.application.port.outbound.SessionInfo
import io.clroot.selah.domains.member.application.port.outbound.SessionPort
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

/**
 * Session Persistence Adapter (DB 기반)
 *
 * SessionPort를 구현합니다.
 * 추후 Redis로 전환 시 이 클래스를 SessionRedisAdapter로 교체합니다.
 */
@Component
class SessionPersistenceAdapter(
    private val repository: SessionJpaRepository,
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
    ): SessionInfo = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now()
        val token = UUID.randomUUID().toString()
        val expiresAt = now.plus(sessionTtl)

        val entity = SessionEntity(
            token = token,
            memberId = memberId.value,
            role = role,
            userAgent = userAgent?.take(500), // 최대 500자
            createdIp = ipAddress?.take(45),
            lastAccessedIp = ipAddress?.take(45),
            expiresAt = expiresAt,
            createdAt = now,
        )

        repository.save(entity)
        entity.toSessionInfo()
    }

    override suspend fun findByToken(token: String): SessionInfo? = withContext(Dispatchers.IO) {
        repository.findByIdOrNull(token)?.toSessionInfo()
    }

    @Transactional
    override suspend fun delete(token: String) {
        withContext(Dispatchers.IO) {
            repository.deleteById(token)
        }
    }

    @Transactional
    override suspend fun deleteAllByMemberId(memberId: MemberId) {
        withContext(Dispatchers.IO) {
            repository.deleteAllByMemberId(memberId.value)
        }
    }

    @Transactional
    override suspend fun extendExpiry(token: String, ipAddress: String?) {
        withContext(Dispatchers.IO) {
            val entity = repository.findByIdOrNull(token) ?: return@withContext
            val now = LocalDateTime.now()
            val remainingTime = Duration.between(now, entity.expiresAt)

            // 마지막 접근 IP 업데이트
            entity.lastAccessedIp = ipAddress?.take(45)

            // 남은 시간이 threshold 이하일 때만 연장
            if (remainingTime <= extendThreshold) {
                entity.expiresAt = now.plus(sessionTtl)
            }
            repository.save(entity)
        }
    }

    @Transactional
    override suspend fun deleteExpiredSessions(): Int = withContext(Dispatchers.IO) {
        repository.deleteExpiredSessions(LocalDateTime.now())
    }

    private fun SessionEntity.toSessionInfo(): SessionInfo = SessionInfo(
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
