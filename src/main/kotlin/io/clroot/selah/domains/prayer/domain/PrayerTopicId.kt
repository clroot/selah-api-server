package io.clroot.selah.domains.prayer.domain

import io.clroot.selah.common.domain.AggregateId
import io.clroot.selah.common.util.ULIDSupport

@JvmInline
value class PrayerTopicId(override val value: String) : AggregateId<String> {
    init {
        require(ULIDSupport.isValidULID(value)) { "Invalid PrayerTopicId format: $value" }
    }

    companion object {
        fun new(): PrayerTopicId = PrayerTopicId(ULIDSupport.generateULID())
        fun from(value: String): PrayerTopicId = PrayerTopicId(value)
    }
}
