package io.clroot.selah.domains.member.domain.event

import io.clroot.selah.common.event.BaseDomainEvent
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.OAuthConnection

/**
 * OAuth 연결 이벤트
 *
 * 사용자가 새로운 OAuth Provider를 연결했을 때 발행됩니다.
 */
data class OAuthConnectedEvent(
    val member: Member,
    val connection: OAuthConnection
) : BaseDomainEvent()
