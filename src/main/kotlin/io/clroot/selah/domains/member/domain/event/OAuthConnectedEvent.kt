package io.clroot.selah.domains.member.domain.event

import io.clroot.selah.common.event.BaseDomainEvent
import io.clroot.selah.domains.member.domain.OAuthConnection

data class OAuthConnectedEvent(
    val connection: OAuthConnection
) : BaseDomainEvent()
