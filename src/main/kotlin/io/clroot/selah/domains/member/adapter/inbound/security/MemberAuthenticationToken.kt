package io.clroot.selah.domains.member.adapter.inbound.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * Member 인증 토큰
 *
 * Spring Security의 Authentication 구현체입니다.
 * 세션 또는 API Key 인증에 사용됩니다.
 */
class MemberAuthenticationToken(
    private val principal: MemberPrincipal,
) : AbstractAuthenticationToken(
        listOf(SimpleGrantedAuthority("ROLE_${principal.role.name}")),
    ) {
    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): MemberPrincipal = principal
}
