package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.spring.data.jpa.extension.createQuery
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.outbound.DeletePrayerTopicPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadPrayerTopicPort
import io.clroot.selah.domains.prayer.application.port.outbound.SavePrayerTopicPort
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.domains.prayer.domain.PrayerTopicStatus
import jakarta.persistence.EntityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * PrayerTopic Persistence Adapter
 *
 * SavePrayerTopicPort, LoadPrayerTopicPort, DeletePrayerTopicPort를 구현합니다.
 * JDSL을 사용하여 타입 안전한 쿼리를 작성합니다.
 */
@Component
class PrayerTopicPersistenceAdapter(
    private val repository: PrayerTopicJpaRepository,
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: PrayerTopicMapper,
) : SavePrayerTopicPort,
    LoadPrayerTopicPort,
    DeletePrayerTopicPort {
    override suspend fun save(prayerTopic: PrayerTopic): PrayerTopic =
        withContext(Dispatchers.IO) {
            val savedEntity =
                if (repository.existsById(prayerTopic.id.value)) {
                    val existingEntity = repository.findById(prayerTopic.id.value).get()
                    mapper.updateEntity(existingEntity, prayerTopic)
                    repository.save(existingEntity)
                } else {
                    repository.save(mapper.toEntity(prayerTopic))
                }
            mapper.toDomain(savedEntity)
        }

    override suspend fun findById(id: PrayerTopicId): PrayerTopic? =
        withContext(Dispatchers.IO) {
            repository.findById(id.value).orElse(null)?.let { mapper.toDomain(it) }
        }

    override suspend fun findByIdAndMemberId(
        id: PrayerTopicId,
        memberId: MemberId,
    ): PrayerTopic? =
        withContext(Dispatchers.IO) {
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
            entityManager
                .createQuery(query, jpqlRenderContext)
                .resultList
                .firstOrNull()
                ?.let { mapper.toDomain(it) }
        }

    override suspend fun findAllByMemberId(
        memberId: MemberId,
        status: PrayerTopicStatus?,
        pageable: Pageable,
    ): Page<PrayerTopic> =
        withContext(Dispatchers.IO) {
            // Count query
            val countQuery =
                jpql {
                    select(count(entity(PrayerTopicEntity::class)))
                        .from(entity(PrayerTopicEntity::class))
                        .whereAnd(
                            path(PrayerTopicEntity::memberId).eq(memberId.value),
                            status?.let { path(PrayerTopicEntity::status).eq(it) },
                        )
                }
            val total =
                entityManager
                    .createQuery(countQuery, jpqlRenderContext)
                    .resultList
                    .firstOrNull() ?: 0L

            // Data query
            val dataQuery =
                jpql {
                    select(entity(PrayerTopicEntity::class))
                        .from(entity(PrayerTopicEntity::class))
                        .whereAnd(
                            path(PrayerTopicEntity::memberId).eq(memberId.value),
                            status?.let { path(PrayerTopicEntity::status).eq(it) },
                        ).orderBy(path(PrayerTopicEntity::createdAt).desc())
                }
            val entities =
                entityManager
                    .createQuery(dataQuery, jpqlRenderContext)
                    .setFirstResult(pageable.offset.toInt())
                    .setMaxResults(pageable.pageSize)
                    .resultList

            PageImpl(entities.map { mapper.toDomain(it) }, pageable, total)
        }

    override suspend fun deleteById(id: PrayerTopicId) =
        withContext(Dispatchers.IO) {
            repository.deleteById(id.value)
        }

    override suspend fun findCandidatesForLookback(
        memberId: MemberId,
        cutoffDate: java.time.LocalDateTime,
        excludeIds: List<PrayerTopicId>,
    ): List<PrayerTopic> =
        withContext(Dispatchers.IO) {
            val excludeIdValues = excludeIds.map { it.value }

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
            entityManager
                .createQuery(query, jpqlRenderContext)
                .resultList
                .map { mapper.toDomain(it) }
        }
}
