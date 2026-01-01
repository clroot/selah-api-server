package io.clroot.selah.domains.prayer.domain

import io.clroot.selah.common.domain.AggregateId
import io.clroot.selah.common.util.ULIDSupport

@JvmInline
value class LookbackSelectionId(
    override val value: String,
) : AggregateId<String> {
    init {
        require(ULIDSupport.isValidULID(value)) { "Invalid LookbackSelectionId format: $value" }
    }

    companion object {
        fun new(): LookbackSelectionId = LookbackSelectionId(ULIDSupport.generateULID())

        fun from(value: String): LookbackSelectionId = LookbackSelectionId(value)
    }
}
