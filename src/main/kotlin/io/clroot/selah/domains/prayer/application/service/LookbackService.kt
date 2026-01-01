package io.clroot.selah.domains.prayer.application.service

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.inbound.GetLookbackUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.LookbackResult
import io.clroot.selah.domains.prayer.application.port.inbound.RefreshLookbackUseCase
import io.clroot.selah.domains.prayer.application.port.outbound.DeleteLookbackSelectionPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadLookbackSelectionPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadPrayerTopicPort
import io.clroot.selah.domains.prayer.application.port.outbound.SaveLookbackSelectionPort
import io.clroot.selah.domains.prayer.domain.LookbackSelection
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.exception.NoEligiblePrayerTopicsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional
class LookbackService(
    private val loadPrayerTopicPort: LoadPrayerTopicPort,
    private val saveLookbackSelectionPort: SaveLookbackSelectionPort,
    private val loadLookbackSelectionPort: LoadLookbackSelectionPort,
    private val deleteLookbackSelectionPort: DeleteLookbackSelectionPort,
) : GetLookbackUseCase,
    RefreshLookbackUseCase {
    override suspend fun getTodayLookback(memberId: MemberId): LookbackResult? {
        val today = LocalDate.now()

        val existingSelection = loadLookbackSelectionPort.findByMemberIdAndDate(memberId, today)
        if (existingSelection != null) {
            return loadPrayerTopicPort
                .findById(existingSelection.prayerTopicId)
                ?.let { buildLookbackResult(it, existingSelection.selectedAt) }
        }

        return try {
            selectAndSave(memberId, today)
        } catch (_: NoEligiblePrayerTopicsException) {
            null
        }
    }

    override suspend fun refresh(memberId: MemberId): LookbackResult {
        val today = LocalDate.now()

        deleteLookbackSelectionPort.deleteByMemberIdAndDate(memberId, today)

        return selectAndSave(memberId, today)
    }

    private suspend fun selectAndSave(
        memberId: MemberId,
        date: LocalDate,
    ): LookbackResult {
        val recentIds = loadLookbackSelectionPort.findRecentPrayerTopicIds(memberId, RECENT_SELECTION_DAYS)

        val cutoffDate = LocalDateTime.now().minusDays(MIN_DAYS_OLD.toLong())
        val candidates = loadPrayerTopicPort.findCandidatesForLookback(memberId, cutoffDate, recentIds)

        if (candidates.isEmpty()) {
            throw NoEligiblePrayerTopicsException(memberId.value)
        }

        val selected = candidates.random()

        val selection = LookbackSelection.create(memberId, selected.id)
        saveLookbackSelectionPort.save(selection)

        return buildLookbackResult(selected, date)
    }

    private fun buildLookbackResult(
        prayerTopic: PrayerTopic,
        selectedAt: LocalDate,
    ): LookbackResult {
        val daysSinceCreated = ChronoUnit.DAYS.between(prayerTopic.createdAt.toLocalDate(), LocalDate.now())
        return LookbackResult(
            prayerTopic = prayerTopic,
            selectedAt = selectedAt,
            daysSinceCreated = daysSinceCreated,
        )
    }

    companion object {
        private const val MIN_DAYS_OLD = 7
        private const val RECENT_SELECTION_DAYS = 7
    }
}
