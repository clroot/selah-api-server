package io.clroot.selah.domains.member.adapter.outbound.persistence.encryption

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.hibernate.reactive.ReactiveSessionProvider
import io.clroot.selah.domains.member.application.port.outbound.DeleteEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.LoadEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.SaveEncryptionSettingsPort
import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId
import org.springframework.stereotype.Component

@Component
class EncryptionSettingsPersistenceAdapter(
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: EncryptionSettingsMapper,
) : LoadEncryptionSettingsPort,
    SaveEncryptionSettingsPort,
    DeleteEncryptionSettingsPort {
    override suspend fun findByMemberId(memberId: MemberId): EncryptionSettings? =
        sessions.read { session ->
            session
                .createQuery(
                    jpql {
                        select(entity(EncryptionSettingsEntity::class))
                            .from(entity(EncryptionSettingsEntity::class))
                            .where(path(EncryptionSettingsEntity::memberId).eq(memberId.value))
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
                        select(count(entity(EncryptionSettingsEntity::class)))
                            .from(entity(EncryptionSettingsEntity::class))
                            .where(path(EncryptionSettingsEntity::memberId).eq(memberId.value))
                    },
                    jpqlRenderContext,
                ).singleResult
                .map { count: Long -> count > 0 }
        }

    override suspend fun save(encryptionSettings: EncryptionSettings): EncryptionSettings =
        sessions.write { session ->
            session
                .createQuery(
                    jpql {
                        select(entity(EncryptionSettingsEntity::class))
                            .from(entity(EncryptionSettingsEntity::class))
                            .where(path(EncryptionSettingsEntity::id).eq(encryptionSettings.id.value))
                    },
                    jpqlRenderContext,
                ).singleResultOrNull
                .chain { existing: EncryptionSettingsEntity? ->
                    if (existing != null) {
                        mapper.updateEntity(existing, encryptionSettings)
                        session.merge(existing)
                    } else {
                        val newEntity = mapper.toEntity(encryptionSettings)
                        session.persist(newEntity).replaceWith(newEntity)
                    }
                }.map { mapper.toDomain(it) }
        }

    override suspend fun deleteByMemberId(memberId: MemberId) {
        sessions.write { session ->
            session
                .createMutationQuery(
                    jpql {
                        deleteFrom(entity(EncryptionSettingsEntity::class))
                            .where(path(EncryptionSettingsEntity::memberId).eq(memberId.value))
                    },
                    jpqlRenderContext,
                ).executeUpdate()
        }
    }
}
