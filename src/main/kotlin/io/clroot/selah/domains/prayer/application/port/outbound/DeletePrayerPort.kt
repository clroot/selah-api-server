package io.clroot.selah.domains.prayer.application.port.outbound

import io.clroot.selah.domains.prayer.domain.PrayerId

interface DeletePrayerPort {
    suspend fun deleteById(id: PrayerId)
}
