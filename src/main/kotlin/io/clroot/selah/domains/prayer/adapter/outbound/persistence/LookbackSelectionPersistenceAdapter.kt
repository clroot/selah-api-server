package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.spring.data.jpa.extension.createQuery
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.outbound.DeleteLookbackSelectionPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadLookbackSelectionPort
import io.clroot.selah.domains.prayer.application.port.outbound.SaveLookbackSelectionPort
import io.clroot.selah.domains.prayer.domain.LookbackSelection
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import jakarta.persistence.EntityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class LookbackSelectionPersistenceAdapter(
    private val repository: LookbackSelectionJpaRepository,
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: LookbackSelectionMapper,
) : SaveLookbackSelectionPort,
    LoadLookbackSelectionPort,
    DeleteLookbackSelectionPort {
    override suspend fun save(selection: LookbackSelection): LookbackSelection =
        withContext(Dispatchers.IO) {
            val entity = mapper.toEntity(selection)
            val saved = repository.save(entity)
            mapper.toDomain(saved)
        }

    override suspend fun findByMemberIdAndDate(
        memberId: MemberId,
        date: LocalDate,
    ): LookbackSelection? =
        withContext(Dispatchers.IO) {
            val query =
                jpql {
                    select(entity(LookbackSelectionEntity::class))
                        .from(entity(LookbackSelectionEntity::class))
                        .where(
                            and(
                                path(LookbackSelectionEntity::memberId).eq(memberId.value),
                                path(LookbackSelectionEntity::selectedAt).eq(date),
                            ),
                        )
                }
            entityManager
                .createQuery(query, jpqlRenderContext)
                .resultList
                .firstOrNull()
                ?.let { mapper.toDomain(it) }
        }

    override suspend fun findRecentPrayerTopicIds(
        memberId: MemberId,
        days: Int,
    ): List<PrayerTopicId> =
        withContext(Dispatchers.IO) {
            val cutoffDate = LocalDate.now().minusDays(days.toLong())
            val query =
                jpql {
                    select(path(LookbackSelectionEntity::prayerTopicId))
                        .from(entity(LookbackSelectionEntity::class))
                        .where(
                            and(
                                path(LookbackSelectionEntity::memberId).eq(memberId.value),
                                path(LookbackSelectionEntity::selectedAt).ge(cutoffDate),
                            ),
                        )
                }
            entityManager
                .createQuery(query, jpqlRenderContext)
                .resultList
                .map { PrayerTopicId.from(it) }
        }

    override suspend fun deleteByMemberIdAndDate(
        memberId: MemberId,
        date: LocalDate,
    ) = withContext(Dispatchers.IO) {
        repository.deleteByMemberIdAndSelectedAt(memberId.value, date)
    }
}
