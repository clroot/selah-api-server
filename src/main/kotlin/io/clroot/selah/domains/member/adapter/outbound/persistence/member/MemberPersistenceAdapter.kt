package io.clroot.selah.domains.member.adapter.outbound.persistence.member

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.hibernate.reactive.ReactiveSessionProvider
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.SaveMemberPort
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthProvider
import org.springframework.stereotype.Component

/**
 * Member Persistence Adapter
 *
 * LoadMemberPort와 SaveMemberPort를 구현합니다.
 * JDSL을 사용하여 타입 안전한 쿼리를 작성합니다.
 */
@Component
class MemberPersistenceAdapter(
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: MemberMapper,
) : LoadMemberPort,
    SaveMemberPort {
    override suspend fun findById(memberId: MemberId): Member? =
        sessions.read { session ->
            session
                .createQuery(
                    jpql {
                        selectDistinct(entity(MemberEntity::class))
                            .from(
                                entity(MemberEntity::class),
                                leftFetchJoin(MemberEntity::oauthConnections),
                            ).where(path(MemberEntity::id).eq(memberId.value))
                    },
                    jpqlRenderContext,
                ).singleResultOrNull
                .map { it?.let { entity -> mapper.toDomain(entity) } }
        }

    override suspend fun findByEmail(email: Email): Member? =
        sessions.read { session ->
            session
                .createQuery(
                    jpql {
                        selectDistinct(entity(MemberEntity::class))
                            .from(
                                entity(MemberEntity::class),
                                leftFetchJoin(MemberEntity::oauthConnections),
                            ).where(path(MemberEntity::email).eq(email.value))
                    },
                    jpqlRenderContext,
                ).singleResultOrNull
                .map { it?.let { entity -> mapper.toDomain(entity) } }
        }

    override suspend fun findByOAuthConnection(
        provider: OAuthProvider,
        providerId: String,
    ): Member? =
        sessions.read { session ->
            session
                .createQuery(
                    jpql {
                        select(entity(MemberEntity::class))
                            .from(
                                entity(MemberEntity::class),
                                leftFetchJoin(MemberEntity::oauthConnections),
                            ).where(
                                and(
                                    path(OAuthConnectionEntity::provider).eq(provider),
                                    path(OAuthConnectionEntity::providerId).eq(providerId),
                                ),
                            )
                    },
                    jpqlRenderContext,
                ).singleResultOrNull
                .map { it?.let { entity -> mapper.toDomain(entity) } }
        }

    override suspend fun existsByEmail(email: Email): Boolean =
        sessions.read { session ->
            session
                .createQuery(
                    jpql {
                        select(count(entity(MemberEntity::class)))
                            .from(entity(MemberEntity::class))
                            .where(path(MemberEntity::email).eq(email.value))
                    },
                    jpqlRenderContext,
                ).singleResultOrNull
                .map { count: Long -> count > 0 }
        }

    override suspend fun save(member: Member): Member =
        sessions.write { session ->
            session
                .createQuery(
                    jpql {
                        selectDistinct(entity(MemberEntity::class))
                            .from(
                                entity(MemberEntity::class),
                                leftFetchJoin(MemberEntity::oauthConnections),
                            ).where(path(MemberEntity::id).eq(member.id.value))
                    },
                    jpqlRenderContext,
                ).singleResultOrNull
                .chain { existing: MemberEntity? ->
                    if (existing != null) {
                        // Update
                        mapper.updateEntity(existing, member)
                        session.merge(existing)
                    } else {
                        // Insert
                        val newEntity = mapper.toEntity(member)
                        session.persist(newEntity).replaceWith(newEntity)
                    }
                }.map { mapper.toDomain(it) }
        }
}
