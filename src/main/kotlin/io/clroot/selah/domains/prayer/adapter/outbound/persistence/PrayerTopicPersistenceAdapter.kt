package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.hibernate.reactive.ReactiveSessionProvider
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

/**
 * PrayerTopic Persistence Adapter
 *
 * 단순 CRUD는 CoroutineCrudRepository를 사용하고,
 * 복잡한 쿼리(동적 조건, 페이지네이션 등)는 JDSL을 사용합니다.
 */
@Component
class PrayerTopicPersistenceAdapter(
    private val repository: PrayerTopicEntityRepository,
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: PrayerTopicMapper,
) : SavePrayerTopicPort,
    LoadPrayerTopicPort,
    DeletePrayerTopicPort {
    override suspend fun save(prayerTopic: PrayerTopic): PrayerTopic {
        val saved = repository.save(mapper.toEntity(prayerTopic))
        return mapper.toDomain(saved)
    }

    override suspend fun findById(id: PrayerTopicId): PrayerTopic? =
        repository.findById(id.value)?.let { mapper.toDomain(it) }

    override suspend fun findByIdAndMemberId(
        id: PrayerTopicId,
        memberId: MemberId,
    ): PrayerTopic? =
        sessions.read { session ->
            val query =
                jpql {
                    select(entity(PrayerTopicEntity::class))
                        .from(entity(PrayerTopicEntity::class))
                        .where(
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

    override suspend fun deleteById(id: PrayerTopicId) {
        repository.deleteById(id.value)
    }

    // 동적 조건 + 페이지네이션이 필요한 복잡한 쿼리 - JDSL 사용
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

    // 동적 조건이 필요한 복잡한 쿼리 - JDSL 사용
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
