package io.clroot.selah.domains.prayer.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.Prayer
import io.clroot.selah.domains.prayer.domain.PrayerTopicId

interface CreatePrayerUseCase {
    suspend fun create(command: CreatePrayerCommand): Prayer
}

data class CreatePrayerCommand(
    val memberId: MemberId,
    val prayerTopicIds: List<PrayerTopicId>,
    val content: String,
)
