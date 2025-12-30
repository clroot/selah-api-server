package io.clroot.selah.domains.prayer.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.PrayerId

interface DeletePrayerUseCase {
    suspend fun delete(id: PrayerId, memberId: MemberId)
}
