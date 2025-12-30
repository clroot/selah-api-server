package io.clroot.selah.domains.prayer.application.port.outbound

import io.clroot.selah.domains.prayer.domain.Prayer

interface SavePrayerPort {
    suspend fun save(prayer: Prayer): Prayer
}
