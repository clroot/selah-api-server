package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.outbound.DeletePrayerTopicPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadPrayerTopicPort
import io.clroot.selah.domains.prayer.application.port.outbound.SavePrayerTopicPort
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.domains.prayer.domain.PrayerTopicStatus
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class PrayerTopicPersistenceAdapter(
    private val sessionFactory: Mutiny.SessionFactory,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: PrayerTopicMapper,
) : SavePrayerTopicPort,
    LoadPrayerTopicPort,
    DeletePrayerTopicPort {
    override suspend fun save(prayerTopic: PrayerTopic): PrayerTopic =
        sessionFactory
            .withTransaction { session ->
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
                    }
            }.awaitSuspending()
            .let { mapper.toDomain(it) }

    override suspend fun findById(id: PrayerTopicId): PrayerTopic? =
        sessionFactory
            .withSession { session ->
                session.find(PrayerTopicEntity::class.java, id.value)
            }.awaitSuspending()
            ?.let { mapper.toDomain(it) }

    override suspend fun findByIdAndMemberId(
        id: PrayerTopicId,
        memberId: MemberId,
    ): PrayerTopic? =
        sessionFactory
            .withSession { session ->
                session
                    .createQuery(
                        "from PrayerTopicEntity where id = :id and memberId = :memberId",
                        PrayerTopicEntity::class.java,
                    ).setParameter("id", id.value)
                    .setParameter("memberId", memberId.value)
                    .singleResultOrNull
            }.awaitSuspending()
            ?.let { mapper.toDomain(it) }

    override suspend fun findAllByMemberId(
        memberId: MemberId,
        status: PrayerTopicStatus?,
        pageable: Pageable,
    ): Page<PrayerTopic> =
        sessionFactory
            .withSession { session ->
                val countQuery =
                    if (status == null) {
                        session
                            .createQuery(
                                "select count(e) from PrayerTopicEntity e where e.memberId = :memberId",
                                Long::class.java,
                            ).setParameter("memberId", memberId.value)
                            .singleResult
                    } else {
                        session
                            .createQuery(
                                "select count(e) from PrayerTopicEntity e where e.memberId = :memberId and e.status = :status",
                                Long::class.java,
                            ).setParameter("memberId", memberId.value)
                            .setParameter("status", status)
                            .singleResult
                    }

                countQuery.chain { total: Long ->
                    val dataQuery =
                        if (status == null) {
                            session
                                .createQuery(
                                    "from PrayerTopicEntity where memberId = :memberId order by createdAt desc",
                                    PrayerTopicEntity::class.java,
                                ).setParameter("memberId", memberId.value)
                        } else {
                            session
                                .createQuery(
                                    "from PrayerTopicEntity where memberId = :memberId and status = :status order by createdAt desc",
                                    PrayerTopicEntity::class.java,
                                ).setParameter("memberId", memberId.value)
                                .setParameter("status", status)
                        }
                    dataQuery
                        .setFirstResult(pageable.offset.toInt())
                        .setMaxResults(pageable.pageSize)
                        .resultList
                        .map { entities: List<PrayerTopicEntity> ->
                            PageImpl(entities.map { mapper.toDomain(it) }, pageable, total)
                        }
                }
            }.awaitSuspending()

    override suspend fun deleteById(id: PrayerTopicId) {
        sessionFactory
            .withTransaction { session ->
                session
                    .find(PrayerTopicEntity::class.java, id.value)
                    .chain { entity: PrayerTopicEntity? ->
                        if (entity != null) {
                            session.remove(entity).chain { _ -> session.flush() }
                        } else {
                            Uni.createFrom().voidItem()
                        }
                    }
            }.awaitSuspending()
    }

    override suspend fun findCandidatesForLookback(
        memberId: MemberId,
        cutoffDate: LocalDateTime,
        excludeIds: List<PrayerTopicId>,
    ): List<PrayerTopic> {
        val excludeIdValues = excludeIds.map { it.value }

        return sessionFactory
            .withSession { session ->
                val query =
                    jpql {
                        select(entity(PrayerTopicEntity::class))
                            .from(entity(PrayerTopicEntity::class))
                            .whereAnd(
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
                    .createQuery(query, jpqlRenderContext)
                    .resultList
                    .map { entities: List<PrayerTopicEntity> -> entities.map { mapper.toDomain(it) } }
            }.awaitSuspending()
    }
}
