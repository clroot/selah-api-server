package io.clroot.selah.domains.prayer.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.Prayer
import io.clroot.selah.domains.prayer.domain.PrayerId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface GetPrayerUseCase {
    suspend fun getById(
        id: PrayerId,
        memberId: MemberId,
    ): Prayer

    suspend fun listByMemberId(
        memberId: MemberId,
        pageable: Pageable,
    ): Page<Prayer>
}
