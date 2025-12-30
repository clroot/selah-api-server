package io.clroot.selah.domains.prayer.application.port.outbound

import io.clroot.selah.domains.prayer.domain.PrayerTopicId

interface DeletePrayerTopicPort {
    suspend fun deleteById(id: PrayerTopicId)
}
