package io.clroot.selah.domains.member.domain.event

import io.clroot.selah.common.event.BaseDomainEvent
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.OAuthProvider

/**
 * OAuth 연결 해제 이벤트
 *
 * 사용자가 OAuth Provider 연결을 해제했을 때 발행됩니다.
 */
data class OAuthDisconnectedEvent(
    val member: Member,
    val disconnectedProvider: OAuthProvider,
) : BaseDomainEvent()
