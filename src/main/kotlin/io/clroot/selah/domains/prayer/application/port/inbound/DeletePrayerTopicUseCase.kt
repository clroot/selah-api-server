package io.clroot.selah.domains.prayer.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.PrayerTopicId

interface DeletePrayerTopicUseCase {
    suspend fun delete(
        id: PrayerTopicId,
        memberId: MemberId,
    )
}
