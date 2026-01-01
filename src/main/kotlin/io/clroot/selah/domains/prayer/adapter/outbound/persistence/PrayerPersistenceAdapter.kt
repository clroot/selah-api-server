package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.spring.data.jpa.extension.createQuery
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.outbound.DeletePrayerPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadPrayerPort
import io.clroot.selah.domains.prayer.application.port.outbound.SavePrayerPort
import io.clroot.selah.domains.prayer.domain.Prayer
import io.clroot.selah.domains.prayer.domain.PrayerId
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import jakarta.persistence.EntityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * Prayer Persistence Adapter
 *
 * SavePrayerPort, LoadPrayerPort, DeletePrayerPort를 구현합니다.
 * JDSL을 사용하여 타입 안전한 쿼리를 작성합니다.
 */
@Component
class PrayerPersistenceAdapter(
    private val repository: PrayerJpaRepository,
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: PrayerMapper,
) : SavePrayerPort,
    LoadPrayerPort,
    DeletePrayerPort {
    override suspend fun save(prayer: Prayer): Prayer =
        withContext(Dispatchers.IO) {
            val savedEntity =
                if (repository.existsById(prayer.id.value)) {
                    val existingEntity = repository.findById(prayer.id.value).get()
                    mapper.updateEntity(existingEntity, prayer)
                    repository.save(existingEntity)
                } else {
                    repository.save(mapper.toEntity(prayer))
                }
            mapper.toDomain(savedEntity)
        }

    override suspend fun findById(id: PrayerId): Prayer? =
        withContext(Dispatchers.IO) {
            repository.findById(id.value).orElse(null)?.let { mapper.toDomain(it) }
        }

    override suspend fun findByIdAndMemberId(
        id: PrayerId,
        memberId: MemberId,
    ): Prayer? =
        withContext(Dispatchers.IO) {
            val query =
                jpql {
                    select(entity(PrayerEntity::class))
                        .from(entity(PrayerEntity::class))
                        .where(
                            and(
                                path(PrayerEntity::id).eq(id.value),
                                path(PrayerEntity::memberId).eq(memberId.value),
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
        pageable: Pageable,
    ): Page<Prayer> =
        withContext(Dispatchers.IO) {
            // Count query
            val countQuery =
                jpql {
                    select(count(entity(PrayerEntity::class)))
                        .from(entity(PrayerEntity::class))
                        .where(path(PrayerEntity::memberId).eq(memberId.value))
                }
            val total =
                entityManager
                    .createQuery(countQuery, jpqlRenderContext)
                    .resultList
                    .firstOrNull() ?: 0L

            // Data query
            val dataQuery =
                jpql {
                    select(entity(PrayerEntity::class))
                        .from(entity(PrayerEntity::class))
                        .where(path(PrayerEntity::memberId).eq(memberId.value))
                        .orderBy(path(PrayerEntity::createdAt).desc())
                }
            val entities =
                entityManager
                    .createQuery(dataQuery, jpqlRenderContext)
                    .setFirstResult(pageable.offset.toInt())
                    .setMaxResults(pageable.pageSize)
                    .resultList

            PageImpl(entities.map { mapper.toDomain(it) }, pageable, total)
        }

    override suspend fun deleteById(id: PrayerId) =
        withContext(Dispatchers.IO) {
            repository.deleteById(id.value)
        }

    override suspend fun findAllByMemberIdAndPrayerTopicId(
        memberId: MemberId,
        prayerTopicId: PrayerTopicId,
        pageable: Pageable,
    ): Page<Prayer> =
        withContext(Dispatchers.IO) {
            val countQuery =
                jpql {
                    val prayerIdsWithTopic =
                        select(path(PrayerPrayerTopicEntity::prayerId))
                            .from(entity(PrayerPrayerTopicEntity::class))
                            .where(path(PrayerPrayerTopicEntity::prayerTopicId).eq(prayerTopicId.value))
                            .asSubquery()

                    select(count(entity(PrayerEntity::class)))
                        .from(entity(PrayerEntity::class))
                        .where(
                            and(
                                path(PrayerEntity::memberId).eq(memberId.value),
                                path(PrayerEntity::id).`in`(prayerIdsWithTopic),
                            ),
                        )
                }
            val total =
                entityManager
                    .createQuery(countQuery, jpqlRenderContext)
                    .resultList
                    .firstOrNull() ?: 0L

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
            val entities =
                entityManager
                    .createQuery(dataQuery, jpqlRenderContext)
                    .setFirstResult(pageable.offset.toInt())
                    .setMaxResults(pageable.pageSize)
                    .resultList

            PageImpl(entities.map { mapper.toDomain(it) }, pageable, total)
        }

    override suspend fun countByPrayerTopicIds(prayerTopicIds: List<PrayerTopicId>): Map<PrayerTopicId, Long> =
        withContext(Dispatchers.IO) {
            if (prayerTopicIds.isEmpty()) {
                return@withContext emptyMap()
            }

            val topicIdValues = prayerTopicIds.map { it.value }

            data class PrayerTopicPrayerCount(
                val prayerTopicId: String,
                val count: Long,
            )

            val query =
                jpql {
                    selectNew<PrayerTopicPrayerCount>(
                        path(PrayerPrayerTopicEntity::prayerTopicId),
                        count(path(PrayerPrayerTopicEntity::prayerId)),
                    ).from(entity(PrayerPrayerTopicEntity::class))
                        .where(path(PrayerPrayerTopicEntity::prayerTopicId).`in`(topicIdValues))
                        .groupBy(path(PrayerPrayerTopicEntity::prayerTopicId))
                }

            entityManager
                .createQuery(query, jpqlRenderContext)
                .resultList
                .associate { PrayerTopicId.from(it.prayerTopicId) to it.count }
        }
}
