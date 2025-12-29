package io.clroot.selah.domains.member.adapter.outbound.persistence.member

import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.SaveMemberPort
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

/**
 * Member Persistence Adapter
 *
 * LoadMemberPort와 SaveMemberPort를 구현합니다.
 * JPA 호출은 Dispatchers.IO에서 실행됩니다.
 */
@Component
class MemberPersistenceAdapter(
    private val repository: MemberJpaRepository,
    private val mapper: MemberMapper,
) : LoadMemberPort, SaveMemberPort {

    override suspend fun findById(memberId: MemberId): Member? = withContext(Dispatchers.IO) {
        repository.findByIdWithOAuthConnections(memberId.value)?.let { mapper.toDomain(it) }
    }

    override suspend fun findByEmail(email: Email): Member? = withContext(Dispatchers.IO) {
        repository.findByEmailWithOAuthConnections(email.value)?.let { mapper.toDomain(it) }
    }

    override suspend fun findByOAuthConnection(
        provider: OAuthProvider,
        providerId: String,
    ): Member? = withContext(Dispatchers.IO) {
        repository.findByOAuthConnection(provider, providerId)?.let { mapper.toDomain(it) }
    }

    override suspend fun existsByEmail(email: Email): Boolean = withContext(Dispatchers.IO) {
        repository.existsByEmail(email.value)
    }

    override suspend fun save(member: Member): Member = withContext(Dispatchers.IO) {
        val existingEntity = repository.findByIdWithOAuthConnections(member.id.value)

        val savedEntity = if (existingEntity != null) {
            // 기존 Entity 업데이트
            mapper.updateEntity(existingEntity, member)
            repository.save(existingEntity)
        } else {
            // 새 Entity 저장
            val newEntity = mapper.toEntity(member)
            repository.save(newEntity)
        }

        mapper.toDomain(savedEntity)
    }
}
