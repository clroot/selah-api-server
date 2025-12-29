package io.clroot.selah.domains.member.domain.event

import io.clroot.selah.common.event.BaseDomainEvent
import io.clroot.selah.domains.member.domain.Member

data class MemberCreatedEvent(
    val member: Member
) : BaseDomainEvent()
