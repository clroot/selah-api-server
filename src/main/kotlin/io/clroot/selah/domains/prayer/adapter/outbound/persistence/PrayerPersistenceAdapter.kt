package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
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

/**
 * Prayer Persistence Adapter
 *
 * 단순 CRUD는 CoroutineCrudRepository를 사용하고,
 * 복잡한 쿼리(서브쿼리, GROUP BY 등)는 JDSL을 사용합니다.
 */
@Component
class PrayerPersistenceAdapter(
    private val repository: PrayerEntityRepository,
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: PrayerMapper,
) : SavePrayerPort,
    LoadPrayerPort,
    DeletePrayerPort {
    override suspend fun save(prayer: Prayer): Prayer {
        val saved = repository.save(mapper.toEntity(prayer))
        return mapper.toDomain(saved)
    }

    override suspend fun findById(id: PrayerId): Prayer? = repository.findById(id.value)?.let { mapper.toDomain(it) }

    override suspend fun findByIdAndMemberId(
        id: PrayerId,
        memberId: MemberId,
    ): Prayer? = repository.findByIdAndMemberId(id.value, memberId.value)?.let { mapper.toDomain(it) }

    override suspend fun deleteById(id: PrayerId) {
        repository.deleteById(id.value)
    }

    override suspend fun findAllByMemberId(
        memberId: MemberId,
        pageable: Pageable,
    ): Page<Prayer> = repository.findAllByMemberId(memberId.value, pageable).map { mapper.toDomain(it) }

    // 서브쿼리가 필요한 복잡한 쿼리 - JDSL 사용
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

    // GROUP BY가 필요한 복잡한 쿼리 - JDSL 사용
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
