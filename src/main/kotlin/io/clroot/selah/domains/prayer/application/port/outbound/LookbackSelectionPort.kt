package io.clroot.selah.domains.prayer.application.port.outbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.LookbackSelection
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import java.time.LocalDate

interface SaveLookbackSelectionPort {
    suspend fun save(selection: LookbackSelection): LookbackSelection
}

interface LoadLookbackSelectionPort {
    suspend fun findByMemberIdAndDate(
        memberId: MemberId,
        date: LocalDate,
    ): LookbackSelection?

    suspend fun findRecentPrayerTopicIds(
        memberId: MemberId,
        days: Int,
    ): List<PrayerTopicId>
}

interface DeleteLookbackSelectionPort {
    suspend fun deleteByMemberIdAndDate(
        memberId: MemberId,
        date: LocalDate,
    )
}
