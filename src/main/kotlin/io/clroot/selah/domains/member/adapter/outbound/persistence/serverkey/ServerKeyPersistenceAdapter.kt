package io.clroot.selah.domains.member.adapter.outbound.persistence.serverkey

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.selah.domains.member.application.port.outbound.DeleteServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.LoadServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.SaveServerKeyPort
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.ServerKey
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny
import org.springframework.stereotype.Component

@Component
class ServerKeyPersistenceAdapter(
    private val sessionFactory: Mutiny.SessionFactory,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: ServerKeyMapper,
) : LoadServerKeyPort,
    SaveServerKeyPort,
    DeleteServerKeyPort {
    override suspend fun findByMemberId(memberId: MemberId): ServerKey? =
        sessionFactory
            .withSession { session ->
                val query =
                    jpql {
                        select(entity(ServerKeyEntity::class))
                            .from(entity(ServerKeyEntity::class))
                            .where(path(ServerKeyEntity::memberId).eq(memberId.value))
                    }
                session.createQuery(query, jpqlRenderContext).singleResultOrNull
            }.awaitSuspending()
            ?.let { mapper.toDomain(it) }

    override suspend fun existsByMemberId(memberId: MemberId): Boolean =
        sessionFactory
            .withSession { session ->
                val query =
                    jpql {
                        select(count(entity(ServerKeyEntity::class)))
                            .from(entity(ServerKeyEntity::class))
                            .where(path(ServerKeyEntity::memberId).eq(memberId.value))
                    }
                session
                    .createQuery(query, jpqlRenderContext)
                    .singleResult
                    .map { count: Long -> count > 0 }
            }.awaitSuspending()

    override suspend fun save(serverKey: ServerKey): ServerKey =
        sessionFactory
            .withTransaction { session ->
                session
                    .find(ServerKeyEntity::class.java, serverKey.id.value)
                    .chain { existing: ServerKeyEntity? ->
                        if (existing != null) {
                            mapper.updateEntity(existing, serverKey)
                            session.merge(existing)
                        } else {
                            val newEntity = mapper.toEntity(serverKey)
                            session.persist(newEntity).replaceWith(newEntity)
                        }
                    }
            }.awaitSuspending()
            .let { mapper.toDomain(it) }

    override suspend fun deleteByMemberId(memberId: MemberId) {
        sessionFactory
            .withTransaction { session ->
                val query =
                    jpql {
                        deleteFrom(entity(ServerKeyEntity::class))
                            .where(path(ServerKeyEntity::memberId).eq(memberId.value))
                    }
                session.createMutationQuery(query, jpqlRenderContext).executeUpdate()
            }.awaitSuspending()
    }
}
