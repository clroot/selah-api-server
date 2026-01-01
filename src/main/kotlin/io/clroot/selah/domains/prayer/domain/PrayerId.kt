package io.clroot.selah.domains.prayer.domain

import io.clroot.selah.common.domain.AggregateId
import io.clroot.selah.common.util.ULIDSupport

@JvmInline
value class PrayerId(
    override val value: String,
) : AggregateId<String> {
    init {
        require(ULIDSupport.isValidULID(value)) { "Invalid PrayerId format: $value" }
    }

    companion object {
        fun new(): PrayerId = PrayerId(ULIDSupport.generateULID())

        fun from(value: String): PrayerId = PrayerId(value)
    }
}
