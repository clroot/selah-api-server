package io.clroot.selah.domains.member.adapter.outbound.persistence.member

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.predicate.Predicate
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.spring.data.jpa.extension.createQuery
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.SaveMemberPort
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthProvider
import jakarta.persistence.EntityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

/**
 * Member Persistence Adapter
 *
 * LoadMemberPort와 SaveMemberPort를 구현합니다.
 * JDSL을 사용하여 타입 안전한 쿼리를 작성합니다.
 */
@Component
class MemberPersistenceAdapter(
    private val repository: MemberJpaRepository,
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: MemberMapper,
) : LoadMemberPort,
    SaveMemberPort {
    override suspend fun findById(memberId: MemberId): Member? =
        withContext(Dispatchers.IO) {
            findMemberEntityBy { path(MemberEntity::id).eq(memberId.value) }
                ?.let { mapper.toDomain(it) }
        }

    override suspend fun findByEmail(email: Email): Member? =
        withContext(Dispatchers.IO) {
            findMemberEntityBy { path(MemberEntity::email).eq(email.value) }
                ?.let { mapper.toDomain(it) }
        }

    override suspend fun findByOAuthConnection(
        provider: OAuthProvider,
        providerId: String,
    ): Member? =
        withContext(Dispatchers.IO) {
            findMemberEntityBy {
                and(
                    path(OAuthConnectionEntity::provider).eq(provider),
                    path(OAuthConnectionEntity::providerId).eq(providerId),
                )
            }?.let { mapper.toDomain(it) }
        }

    override suspend fun existsByEmail(email: Email): Boolean =
        withContext(Dispatchers.IO) {
            repository.existsByEmail(email.value)
        }

    override suspend fun save(member: Member): Member =
        withContext(Dispatchers.IO) {
            val savedEntity =
                if (repository.existsById(member.id.value)) {
                    val existingEntity = findMemberEntityBy { path(MemberEntity::id).eq(member.id.value) }!!
                    mapper.updateEntity(existingEntity, member)
                    repository.save(existingEntity)
                } else {
                    repository.save(mapper.toEntity(member))
                }

            mapper.toDomain(savedEntity)
        }

    /**
     * 조건에 맞는 MemberEntity를 OAuth 연결과 함께 조회합니다.
     */
    private fun findMemberEntityBy(predicate: Jpql.() -> Predicate): MemberEntity? {
        val query =
            jpql {
                selectDistinct(entity(MemberEntity::class))
                    .from(
                        entity(MemberEntity::class),
                        leftFetchJoin(MemberEntity::oauthConnections),
                    ).where(predicate())
            }

        return entityManager
            .createQuery(query, jpqlRenderContext)
            .resultList
            .firstOrNull()
    }
}
