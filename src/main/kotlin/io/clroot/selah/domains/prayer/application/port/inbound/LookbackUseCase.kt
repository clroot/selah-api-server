package io.clroot.selah.domains.prayer.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import java.time.LocalDate

data class LookbackResult(
    val prayerTopic: PrayerTopic,
    val selectedAt: LocalDate,
    val daysSinceCreated: Long,
)

interface GetLookbackUseCase {
    suspend fun getTodayLookback(memberId: MemberId): LookbackResult?
}

interface RefreshLookbackUseCase {
    suspend fun refresh(memberId: MemberId): LookbackResult
}
