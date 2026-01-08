package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.outbound.DeleteLookbackSelectionPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadLookbackSelectionPort
import io.clroot.selah.domains.prayer.application.port.outbound.SaveLookbackSelectionPort
import io.clroot.selah.domains.prayer.domain.LookbackSelection
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class LookbackSelectionPersistenceAdapter(
    private val sessionFactory: Mutiny.SessionFactory,
    private val mapper: LookbackSelectionMapper,
) : SaveLookbackSelectionPort,
    LoadLookbackSelectionPort,
    DeleteLookbackSelectionPort {
    override suspend fun save(selection: LookbackSelection): LookbackSelection =
        sessionFactory
            .withTransaction { session ->
                val entity = mapper.toEntity(selection)
                session
                    .persist(entity)
                    .chain { _ -> session.flush() }
                    .replaceWith(mapper.toDomain(entity))
            }.awaitSuspending()

    override suspend fun findByMemberIdAndDate(
        memberId: MemberId,
        date: LocalDate,
    ): LookbackSelection? =
        sessionFactory
            .withSession { session ->
                session
                    .createQuery(
                        "from LookbackSelectionEntity where memberId = :memberId and selectedAt = :date",
                        LookbackSelectionEntity::class.java,
                    ).setParameter("memberId", memberId.value)
                    .setParameter("date", date)
                    .singleResultOrNull
            }.awaitSuspending()
            ?.let { mapper.toDomain(it) }

    override suspend fun findRecentPrayerTopicIds(
        memberId: MemberId,
        days: Int,
    ): List<PrayerTopicId> {
        val cutoffDate = LocalDate.now().minusDays(days.toLong())

        return sessionFactory
            .withSession { session ->
                session
                    .createQuery(
                        "select e.prayerTopicId from LookbackSelectionEntity e where e.memberId = :memberId and e.selectedAt >= :cutoffDate",
                        String::class.java,
                    ).setParameter("memberId", memberId.value)
                    .setParameter("cutoffDate", cutoffDate)
                    .resultList
            }.awaitSuspending()
            .map { PrayerTopicId.from(it) }
    }

    override suspend fun deleteByMemberIdAndDate(
        memberId: MemberId,
        date: LocalDate,
    ) {
        sessionFactory
            .withTransaction { session ->
                session
                    .createQuery(
                        "from LookbackSelectionEntity where memberId = :memberId and selectedAt = :date",
                        LookbackSelectionEntity::class.java,
                    ).setParameter("memberId", memberId.value)
                    .setParameter("date", date)
                    .singleResultOrNull
                    .chain { entity: LookbackSelectionEntity? ->
                        if (entity != null) {
                            session.remove(entity).chain { _ -> session.flush() }
                        } else {
                            Uni.createFrom().voidItem()
                        }
                    }
            }.awaitSuspending()
    }
}
