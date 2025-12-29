package io.clroot.selah.domains.member.adapter.inbound.security

import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId

/**
 * 인증된 사용자 정보
 *
 * SecurityContext에 저장되는 사용자 정보입니다.
 * 세션 또는 API Key로 인증된 사용자의 정보를 담고 있습니다.
 */
data class MemberPrincipal(
    val memberId: MemberId,
    val role: Member.Role,
    val authenticationType: AuthenticationType,
) {
    /**
     * 인증 방식
     */
    enum class AuthenticationType {
        SESSION,
        API_KEY,
    }
}
