package io.clroot.selah.domains.member.domain

import io.clroot.selah.common.domain.AggregateId
import io.clroot.selah.common.util.ULIDSupport

@JvmInline
value class EncryptionSettingsId(
    override val value: String,
) : AggregateId<String> {
    init {
        require(ULIDSupport.isValidULID(value)) { "Invalid EncryptionSettingsId format: $value" }
    }

    companion object {
        fun new(): EncryptionSettingsId = EncryptionSettingsId(ULIDSupport.generateULID())

        fun from(value: String): EncryptionSettingsId = EncryptionSettingsId(value)
    }
}
