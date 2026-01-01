package io.clroot.selah.domains.prayer.application.port.outbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.domains.prayer.domain.PrayerTopicStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface LoadPrayerTopicPort {
    suspend fun findById(id: PrayerTopicId): PrayerTopic?

    suspend fun findByIdAndMemberId(
        id: PrayerTopicId,
        memberId: MemberId,
    ): PrayerTopic?

    suspend fun findAllByMemberId(
        memberId: MemberId,
        status: PrayerTopicStatus?,
        pageable: Pageable,
    ): Page<PrayerTopic>

    suspend fun findCandidatesForLookback(
        memberId: MemberId,
        cutoffDate: java.time.LocalDateTime,
        excludeIds: List<PrayerTopicId>,
    ): List<PrayerTopic>
}
