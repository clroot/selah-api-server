package io.clroot.selah.domains.member.domain.event

import io.clroot.selah.common.event.BaseDomainEvent
import io.clroot.selah.domains.member.domain.Member

/**
 * 이메일 인증 완료 이벤트
 *
 * 사용자가 이메일 인증을 완료했을 때 발행됩니다.
 */
data class EmailVerifiedEvent(
    val member: Member,
) : BaseDomainEvent()
