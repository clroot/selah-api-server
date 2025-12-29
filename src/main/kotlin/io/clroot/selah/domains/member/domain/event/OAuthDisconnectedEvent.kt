package io.clroot.selah.domains.member.domain.event

import io.clroot.selah.common.event.BaseDomainEvent
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthConnectionId
import io.clroot.selah.domains.member.domain.OAuthProvider

data class OAuthDisconnectedEvent(
    val connectionId: OAuthConnectionId,
    val memberId: MemberId,
    val provider: OAuthProvider
) : BaseDomainEvent()
