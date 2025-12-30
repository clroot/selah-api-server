package io.clroot.selah.domains.prayer.application.port.outbound

import io.clroot.selah.domains.prayer.domain.PrayerTopic

interface SavePrayerTopicPort {
    suspend fun save(prayerTopic: PrayerTopic): PrayerTopic
}
