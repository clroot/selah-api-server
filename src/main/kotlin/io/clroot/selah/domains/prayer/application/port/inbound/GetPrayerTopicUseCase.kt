package io.clroot.selah.domains.prayer.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.domains.prayer.domain.PrayerTopicStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface GetPrayerTopicUseCase {
    suspend fun getById(
        id: PrayerTopicId,
        memberId: MemberId,
    ): PrayerTopic

    suspend fun listByMemberId(
        memberId: MemberId,
        status: PrayerTopicStatus?,
        pageable: Pageable,
    ): Page<PrayerTopic>
}
