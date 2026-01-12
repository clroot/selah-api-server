package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.hibernate.reactive.ReactiveSessionProvider
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.outbound.DeletePrayerPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadPrayerPort
import io.clroot.selah.domains.prayer.application.port.outbound.SavePrayerPort
import io.clroot.selah.domains.prayer.domain.Prayer
import io.clroot.selah.domains.prayer.domain.PrayerId
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class PrayerPersistenceAdapter(
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: PrayerMapper,
) : SavePrayerPort,
    LoadPrayerPort,
    DeletePrayerPort {
    override suspend fun save(prayer: Prayer): Prayer =
        sessions.write { session ->
            session
                .find(PrayerEntity::class.java, prayer.id.value)
                .chain { existing: PrayerEntity? ->
                    if (existing != null) {
                        mapper.updateEntity(existing, prayer)
                        session.merge(existing)
                    } else {
                        val newEntity = mapper.toEntity(prayer)
                        session.persist(newEntity).replaceWith(newEntity)
                    }
                }.map { mapper.toDomain(it) }
        }

    override suspend fun findById(id: PrayerId): Prayer? =
        sessions.read { session ->
            session
                .find(PrayerEntity::class.java, id.value)
                .map { it?.let { entity -> mapper.toDomain(entity) } }
        }

    override suspend fun findByIdAndMemberId(
        id: PrayerId,
        memberId: MemberId,
    ): Prayer? =
        sessions.read { session ->
            val query =
                jpql {
                    select(entity(PrayerEntity::class)).from(entity(PrayerEntity::class)).where(
                        and(
                            path(PrayerEntity::id).eq(id.value),
                            path(PrayerEntity::memberId).eq(memberId.value),
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
        pageable: Pageable,
    ): Page<Prayer> =
        sessions.read { session ->
            val countQuery =
                jpql {
                    select(count(entity(PrayerEntity::class)))
                        .from(entity(PrayerEntity::class))
                        .where(path(PrayerEntity::memberId).eq(memberId.value))
                }

            session.createQuery(countQuery, jpqlRenderContext).singleResult.chain { total: Long ->
                val dataQuery =
                    jpql {
                        select(entity(PrayerEntity::class))
                            .from(entity(PrayerEntity::class))
                            .where(path(PrayerEntity::memberId).eq(memberId.value))
                            .orderBy(path(PrayerEntity::createdAt).desc())
                    }

                session
                    .createQuery(dataQuery, jpqlRenderContext)
                    .setFirstResult(pageable.offset.toInt())
                    .setMaxResults(pageable.pageSize)
                    .resultList
                    .map { entities: List<PrayerEntity> ->
                        PageImpl(entities.map { mapper.toDomain(it) }, pageable, total)
                    }
            }
        }

    override suspend fun deleteById(id: PrayerId) {
        sessions.write { session ->
            val query =
                jpql {
                    deleteFrom(entity(PrayerEntity::class))
                        .where(path(PrayerEntity::id).eq(id.value))
                }
            session.createMutationQuery(query, jpqlRenderContext).executeUpdate()
        }
    }

    override suspend fun findAllByMemberIdAndPrayerTopicId(
        memberId: MemberId,
        prayerTopicId: PrayerTopicId,
        pageable: Pageable,
    ): Page<Prayer> =
        sessions.read { session ->
            val countQuery =
                jpql {
                    val prayerIdsWithTopic =
                        select(path(PrayerPrayerTopicEntity::prayerId))
                            .from(entity(PrayerPrayerTopicEntity::class))
                            .where(path(PrayerPrayerTopicEntity::prayerTopicId).eq(prayerTopicId.value))
                            .asSubquery()

                    select(count(entity(PrayerEntity::class))).from(entity(PrayerEntity::class)).where(
                        and(
                            path(PrayerEntity::memberId).eq(memberId.value),
                            path(PrayerEntity::id).`in`(prayerIdsWithTopic),
                        ),
                    )
                }

            session.createQuery(countQuery, jpqlRenderContext).singleResult.chain { total: Long ->
                val dataQuery =
                    jpql {
                        val prayerIdsWithTopic =
                            select(path(PrayerPrayerTopicEntity::prayerId))
                                .from(entity(PrayerPrayerTopicEntity::class))
                                .where(path(PrayerPrayerTopicEntity::prayerTopicId).eq(prayerTopicId.value))
                                .asSubquery()

                        select(entity(PrayerEntity::class))
                            .from(entity(PrayerEntity::class))
                            .where(
                                and(
                                    path(PrayerEntity::memberId).eq(memberId.value),
                                    path(PrayerEntity::id).`in`(prayerIdsWithTopic),
                                ),
                            ).orderBy(path(PrayerEntity::createdAt).desc())
                    }

                session
                    .createQuery(dataQuery, jpqlRenderContext)
                    .setFirstResult(pageable.offset.toInt())
                    .setMaxResults(pageable.pageSize)
                    .resultList
                    .map { entities: List<PrayerEntity> ->
                        PageImpl(entities.map { mapper.toDomain(it) }, pageable, total)
                    }
            }
        }

    override suspend fun countByPrayerTopicIds(prayerTopicIds: List<PrayerTopicId>): Map<PrayerTopicId, Long> {
        if (prayerTopicIds.isEmpty()) {
            return emptyMap()
        }

        val topicIdValues = prayerTopicIds.map { it.value }

        data class PrayerTopicPrayerCount(
            val prayerTopicId: String,
            val count: Long,
        )

        return sessions.read { session ->
            val query =
                jpql {
                    selectNew<PrayerTopicPrayerCount>(
                        path(PrayerPrayerTopicEntity::prayerTopicId),
                        count(path(PrayerPrayerTopicEntity::prayerId)),
                    ).from(entity(PrayerPrayerTopicEntity::class))
                        .where(path(PrayerPrayerTopicEntity::prayerTopicId).`in`(topicIdValues))
                        .groupBy(path(PrayerPrayerTopicEntity::prayerTopicId))
                }

            session
                .createQuery(query, jpqlRenderContext)
                .resultList
                .map { results -> results.associate { PrayerTopicId.from(it.prayerTopicId) to it.count } }
        }
    }
}
