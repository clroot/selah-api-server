package io.clroot.selah.domains.member.adapter.outbound.persistence.encryption

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.selah.domains.member.application.port.outbound.DeleteEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.LoadEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.SaveEncryptionSettingsPort
import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny
import org.springframework.stereotype.Component

/**
 * EncryptionSettings Persistence Adapter
 */
@Component
class EncryptionSettingsPersistenceAdapter(
    private val sessionFactory: Mutiny.SessionFactory,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: EncryptionSettingsMapper,
) : LoadEncryptionSettingsPort,
    SaveEncryptionSettingsPort,
    DeleteEncryptionSettingsPort {
    override suspend fun findByMemberId(memberId: MemberId): EncryptionSettings? =
        sessionFactory
            .withSession { session ->
                val query =
                    jpql {
                        select(entity(EncryptionSettingsEntity::class))
                            .from(entity(EncryptionSettingsEntity::class))
                            .where(path(EncryptionSettingsEntity::memberId).eq(memberId.value))
                    }
                session.createQuery(query, jpqlRenderContext).singleResultOrNull
            }.awaitSuspending()
            ?.let { mapper.toDomain(it) }

    override suspend fun existsByMemberId(memberId: MemberId): Boolean =
        sessionFactory
            .withSession { session ->
                val query =
                    jpql {
                        select(entity(EncryptionSettingsEntity::class))
                            .from(entity(EncryptionSettingsEntity::class))
                            .where(path(EncryptionSettingsEntity::memberId).eq(memberId.value))
                    }
                session.createQuery(query, jpqlRenderContext).singleResultOrNull
            }.awaitSuspending() != null

    override suspend fun save(encryptionSettings: EncryptionSettings): EncryptionSettings =
        sessionFactory
            .withTransaction { session ->
                session
                    .find(EncryptionSettingsEntity::class.java, encryptionSettings.id.value)
                    .chain { existing: EncryptionSettingsEntity? ->
                        if (existing != null) {
                            mapper.updateEntity(existing, encryptionSettings)
                            session.merge(existing)
                        } else {
                            val newEntity = mapper.toEntity(encryptionSettings)
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
                        deleteFrom(entity(EncryptionSettingsEntity::class))
                            .where(path(EncryptionSettingsEntity::memberId).eq(memberId.value))
                    }
                session.createMutationQuery(query, jpqlRenderContext).executeUpdate()
            }.awaitSuspending()
    }
}
