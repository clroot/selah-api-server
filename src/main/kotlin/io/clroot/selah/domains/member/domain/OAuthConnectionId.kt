package io.clroot.selah.domains.member.domain

import io.clroot.selah.common.domain.AggregateId
import io.clroot.selah.common.util.ULIDSupport

@JvmInline
value class OAuthConnectionId(
    override val value: String,
) : AggregateId<String> {
    init {
        require(ULIDSupport.isValidULID(value)) { "Invalid OAuthConnectionId format: $value" }
    }

    companion object {
        fun new(): OAuthConnectionId = OAuthConnectionId(ULIDSupport.generateULID())

        fun from(value: String): OAuthConnectionId = OAuthConnectionId(value)
    }
}
