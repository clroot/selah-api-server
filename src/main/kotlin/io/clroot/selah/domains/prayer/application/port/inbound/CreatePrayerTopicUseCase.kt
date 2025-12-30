package io.clroot.selah.domains.prayer.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.PrayerTopic

interface CreatePrayerTopicUseCase {
    suspend fun create(command: CreatePrayerTopicCommand): PrayerTopic
}

data class CreatePrayerTopicCommand(
    val memberId: MemberId,
    val title: String,
)
