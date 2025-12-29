package io.clroot.selah.domains.member.domain.event

import io.clroot.selah.common.event.BaseDomainEvent
import io.clroot.selah.domains.member.domain.Member

/**
 * 비밀번호 설정 이벤트
 *
 * OAuth로 가입한 사용자가 비밀번호를 추가로 설정했을 때 발행됩니다.
 */
data class PasswordSetEvent(
    val member: Member
) : BaseDomainEvent()
