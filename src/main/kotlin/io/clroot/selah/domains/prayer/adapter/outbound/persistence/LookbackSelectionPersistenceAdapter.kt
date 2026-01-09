package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.selah.common.reactive.ReactiveSessionProvider
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.outbound.DeleteLookbackSelectionPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadLookbackSelectionPort
import io.clroot.selah.domains.prayer.application.port.outbound.SaveLookbackSelectionPort
import io.clroot.selah.domains.prayer.domain.LookbackSelection
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class LookbackSelectionPersistenceAdapter(
    private val sessions: ReactiveSessionProvider,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: LookbackSelectionMapper,
) : SaveLookbackSelectionPort,
    LoadLookbackSelectionPort,
    DeleteLookbackSelectionPort {
    override suspend fun save(selection: LookbackSelection): LookbackSelection =
        sessions.write { session ->
            val entity = mapper.toEntity(selection)
            session
                .persist(entity)
                .chain { _ -> session.flush() }
                .replaceWith(mapper.toDomain(entity))
        }

    override suspend fun findByMemberIdAndDate(
        memberId: MemberId,
        date: LocalDate,
    ): LookbackSelection? =
        sessions.read { session ->
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
            session
                .createQuery(query, jpqlRenderContext)
                .singleResultOrNull
                .map { it?.let { entity -> mapper.toDomain(entity) } }
        }

    override suspend fun findRecentPrayerTopicIds(
        memberId: MemberId,
        days: Int,
    ): List<PrayerTopicId> {
        val cutoffDate = LocalDate.now().minusDays(days.toLong())

        return sessions.read { session ->
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
            session
                .createQuery(query, jpqlRenderContext)
                .resultList
                .map { results -> results.map { PrayerTopicId.from(it) } }
        }
    }

    override suspend fun deleteByMemberIdAndDate(
        memberId: MemberId,
        date: LocalDate,
    ) {
        sessions.write { session ->
            val query =
                jpql {
                    deleteFrom(entity(LookbackSelectionEntity::class))
                        .where(path(LookbackSelectionEntity::selectedAt).eq(date))
                }
            session
                .createMutationQuery(query, jpqlRenderContext)
                .executeUpdate()
        }
    }
}
