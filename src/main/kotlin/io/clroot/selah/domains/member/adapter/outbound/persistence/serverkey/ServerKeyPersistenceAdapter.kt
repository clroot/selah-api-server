package io.clroot.selah.domains.member.adapter.outbound.persistence.serverkey

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.hibernate.reactive.ReactiveSessionProvider
import io.clroot.selah.domains.member.application.port.outbound.DeleteServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.LoadServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.SaveServerKeyPort
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.ServerKey
import org.springframework.stereotype.Component

@Component
class ServerKeyPersistenceAdapter(
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: ServerKeyMapper,
) : LoadServerKeyPort,
    SaveServerKeyPort,
    DeleteServerKeyPort {
    override suspend fun findByMemberId(memberId: MemberId): ServerKey? =
        sessions.read { session ->
            session
                .createQuery(
                    jpql {
                        select(entity(ServerKeyEntity::class))
                            .from(entity(ServerKeyEntity::class))
                            .where(path(ServerKeyEntity::memberId).eq(memberId.value))
                    },
                    jpqlRenderContext,
                ).singleResultOrNull
                .map { it?.let { entity -> mapper.toDomain(entity) } }
        }

    override suspend fun existsByMemberId(memberId: MemberId): Boolean =
        sessions.read { session ->
            session
                .createQuery(
                    jpql {
                        select(count(entity(ServerKeyEntity::class)))
                            .from(entity(ServerKeyEntity::class))
                            .where(path(ServerKeyEntity::memberId).eq(memberId.value))
                    },
                    jpqlRenderContext,
                ).singleResult
                .map { count: Long -> count > 0 }
        }

    override suspend fun save(serverKey: ServerKey): ServerKey =
        sessions.write { session ->
            session
                .createQuery(
                    jpql {
                        select(entity(ServerKeyEntity::class))
                            .from(entity(ServerKeyEntity::class))
                            .where(path(ServerKeyEntity::id).eq(serverKey.id.value))
                    },
                    jpqlRenderContext,
                ).singleResultOrNull
                .chain { existing: ServerKeyEntity? ->
                    if (existing != null) {
                        mapper.updateEntity(existing, serverKey)
                        session.merge(existing)
                    } else {
                        val newEntity = mapper.toEntity(serverKey)
                        session.persist(newEntity).replaceWith(newEntity)
                    }
                }.map { mapper.toDomain(it) }
        }

    override suspend fun deleteByMemberId(memberId: MemberId) {
        sessions.write { session ->
            session
                .createMutationQuery(
                    jpql {
                        deleteFrom(entity(ServerKeyEntity::class))
                            .where(path(ServerKeyEntity::memberId).eq(memberId.value))
                    },
                    jpqlRenderContext,
                ).executeUpdate()
        }
    }
}
