package io.clroot.selah.domains.prayer.application.port.outbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.Prayer
import io.clroot.selah.domains.prayer.domain.PrayerId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface LoadPrayerPort {
    suspend fun findById(id: PrayerId): Prayer?

    suspend fun findByIdAndMemberId(
        id: PrayerId,
        memberId: MemberId,
    ): Prayer?

    suspend fun findAllByMemberId(
        memberId: MemberId,
        pageable: Pageable,
    ): Page<Prayer>
}
