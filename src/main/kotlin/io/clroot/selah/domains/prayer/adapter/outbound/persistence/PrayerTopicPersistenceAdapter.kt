package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.selah.common.reactive.ReactiveSessionProvider
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.outbound.DeletePrayerTopicPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadPrayerTopicPort
import io.clroot.selah.domains.prayer.application.port.outbound.SavePrayerTopicPort
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.domains.prayer.domain.PrayerTopicStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class PrayerTopicPersistenceAdapter(
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: PrayerTopicMapper,
) : SavePrayerTopicPort,
    LoadPrayerTopicPort,
    DeletePrayerTopicPort {
    override suspend fun save(prayerTopic: PrayerTopic): PrayerTopic =
        sessions.write { session ->
            session
                .find(PrayerTopicEntity::class.java, prayerTopic.id.value)
                .chain { existing: PrayerTopicEntity? ->
                    if (existing != null) {
                        mapper.updateEntity(existing, prayerTopic)
                        session.merge(existing)
                    } else {
                        val newEntity = mapper.toEntity(prayerTopic)
                        session.persist(newEntity).replaceWith(newEntity)
                    }
                }.map { mapper.toDomain(it) }
        }

    override suspend fun findById(id: PrayerTopicId): PrayerTopic? =
        sessions.read { session ->
            session
                .find(PrayerTopicEntity::class.java, id.value)
                .map { it?.let { entity -> mapper.toDomain(entity) } }
        }

    override suspend fun findByIdAndMemberId(
        id: PrayerTopicId,
        memberId: MemberId,
    ): PrayerTopic? =
        sessions.read { session ->
            val query =
                jpql {
                    select(entity(PrayerTopicEntity::class)).from(entity(PrayerTopicEntity::class)).where(
                        and(
                            path(PrayerTopicEntity::id).eq(id.value),
                            path(PrayerTopicEntity::memberId).eq(memberId.value),
                        ),
                    )
                }
            session
                .createQuery(query, jpqlRenderContext)
                .singleResultOrNull
                .map { it?.let { entity -> mapper.toDomain(entity) } }
        }

    override suspend fun findAllByMemberId(
        memberId: MemberId,
        status: PrayerTopicStatus?,
        pageable: Pageable,
    ): Page<PrayerTopic> =
        sessions.read { session ->
            val countQuery =
                jpql {
                    select(count(entity(PrayerTopicEntity::class))).from(entity(PrayerTopicEntity::class)).whereAnd(
                        path(PrayerTopicEntity::memberId).eq(memberId.value),
                        status?.let { path(PrayerTopicEntity::status).eq(it) },
                    )
                }

            session.createQuery(countQuery, jpqlRenderContext).singleResult.chain { total: Long ->
                val dataQuery =
                    jpql {
                        select(entity(PrayerTopicEntity::class))
                            .from(entity(PrayerTopicEntity::class))
                            .whereAnd(
                                path(PrayerTopicEntity::memberId).eq(memberId.value),
                                status?.let { path(PrayerTopicEntity::status).eq(it) },
                            ).orderBy(path(PrayerTopicEntity::createdAt).desc())
                    }

                session
                    .createQuery(dataQuery, jpqlRenderContext)
                    .setFirstResult(pageable.offset.toInt())
                    .setMaxResults(pageable.pageSize)
                    .resultList
                    .map { entities: List<PrayerTopicEntity> ->
                        PageImpl(entities.map { mapper.toDomain(it) }, pageable, total)
                    }
            }
        }

    override suspend fun deleteById(id: PrayerTopicId) {
        sessions.write { session ->
            val query =
                jpql {
                    deleteFrom(entity(PrayerTopicEntity::class))
                        .where(path(PrayerTopicEntity::id).eq(id.value))
                }
            session
                .createMutationQuery(query, jpqlRenderContext)
                .executeUpdate()
        }
    }

    override suspend fun findCandidatesForLookback(
        memberId: MemberId,
        cutoffDate: LocalDateTime,
        excludeIds: List<PrayerTopicId>,
    ): List<PrayerTopic> {
        val excludeIdValues = excludeIds.map { it.value }

        return sessions.read { session ->
            val query =
                jpql {
                    select(entity(PrayerTopicEntity::class)).from(entity(PrayerTopicEntity::class)).whereAnd(
                        path(PrayerTopicEntity::memberId).eq(memberId.value),
                        path(PrayerTopicEntity::createdAt).lt(cutoffDate),
                        if (excludeIdValues.isNotEmpty()) {
                            path(PrayerTopicEntity::id).notIn(excludeIdValues)
                        } else {
                            null
                        },
                    )
                }
            session
                .createQuery(
                    query,
                    jpqlRenderContext,
                ).resultList
                .map { entities: List<PrayerTopicEntity> -> entities.map { mapper.toDomain(it) } }
        }
    }
}
