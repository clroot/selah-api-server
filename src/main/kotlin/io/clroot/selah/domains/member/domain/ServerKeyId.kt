package io.clroot.selah.domains.member.domain

import io.clroot.selah.common.domain.AggregateId
import io.clroot.selah.common.util.ULIDSupport

@JvmInline
value class ServerKeyId(override val value: String) : AggregateId<String> {
    init {
        require(ULIDSupport.isValidULID(value)) { "Invalid ServerKeyId format: $value" }
    }

    companion object {
        fun new(): ServerKeyId = ServerKeyId(ULIDSupport.generateULID())
        fun from(value: String): ServerKeyId = ServerKeyId(value)
    }
}
