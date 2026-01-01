package io.clroot.selah.domains.member.domain.event

import io.clroot.selah.common.event.BaseDomainEvent
import io.clroot.selah.domains.member.domain.Member

/**
 * 비밀번호 변경 이벤트
 *
 * 기존 비밀번호를 새 비밀번호로 변경했을 때 발행됩니다.
 */
data class PasswordChangedEvent(
    val member: Member,
) : BaseDomainEvent()
