package io.clroot.selah.domains.member.domain

import io.clroot.selah.common.domain.AggregateId
import io.clroot.selah.common.util.ULIDSupport

@JvmInline
value class MemberId(
    override val value: String,
) : AggregateId<String> {
    init {
        require(ULIDSupport.isValidULID(value)) { "Invalid MemberId format: $value" }
    }

    companion object {
        fun new(): MemberId = MemberId(ULIDSupport.generateULID())

        fun from(value: String): MemberId = MemberId(value)
    }
}
