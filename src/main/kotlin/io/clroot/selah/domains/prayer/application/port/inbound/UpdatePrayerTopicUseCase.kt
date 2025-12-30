package io.clroot.selah.domains.prayer.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId

interface UpdatePrayerTopicUseCase {
    suspend fun updateTitle(command: UpdatePrayerTopicTitleCommand): PrayerTopic
}

data class UpdatePrayerTopicTitleCommand(
    val id: PrayerTopicId,
    val memberId: MemberId,
    val title: String,
)
