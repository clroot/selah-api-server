package io.clroot.selah.domains.prayer.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.Prayer
import io.clroot.selah.domains.prayer.domain.PrayerId

interface UpdatePrayerUseCase {
    suspend fun updateContent(command: UpdatePrayerContentCommand): Prayer
}

data class UpdatePrayerContentCommand(
    val id: PrayerId,
    val memberId: MemberId,
    val content: String,
)
