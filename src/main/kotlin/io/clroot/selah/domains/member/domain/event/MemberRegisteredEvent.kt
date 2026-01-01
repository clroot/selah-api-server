package io.clroot.selah.domains.member.domain.event

import io.clroot.selah.common.event.BaseDomainEvent
import io.clroot.selah.domains.member.domain.Member

/**
 * 회원 가입 이벤트
 *
 * 새로운 회원이 가입했을 때 발행됩니다.
 * 이메일/비밀번호 가입 또는 OAuth 가입 모두에서 발행됩니다.
 */
data class MemberRegisteredEvent(
    val member: Member,
) : BaseDomainEvent()
