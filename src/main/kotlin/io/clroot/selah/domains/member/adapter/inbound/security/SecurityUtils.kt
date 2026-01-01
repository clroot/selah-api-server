package io.clroot.selah.domains.member.adapter.inbound.security

import io.clroot.selah.domains.member.domain.MemberId
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Security 관련 유틸리티
 */
object SecurityUtils {
    /**
     * 현재 인증된 사용자 정보를 반환합니다.
     *
     * @return MemberPrincipal 또는 null (미인증 시)
     */
    fun getCurrentPrincipal(): MemberPrincipal? {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: return null

        if (!authentication.isAuthenticated) {
            return null
        }

        val principal = authentication.principal
        return principal as? MemberPrincipal
    }

    /**
     * 현재 인증된 사용자의 Member ID를 반환합니다.
     *
     * @return MemberId 또는 null (미인증 시)
     */
    fun getCurrentMemberId(): MemberId? = getCurrentPrincipal()?.memberId

    /**
     * 현재 인증된 사용자의 Member ID를 반환합니다.
     * 인증되지 않은 경우 예외를 던집니다.
     *
     * @return MemberId
     * @throws IllegalStateException 미인증 시
     */
    fun requireCurrentMemberId(): MemberId =
        getCurrentMemberId()
            ?: throw IllegalStateException("No authenticated member found")
}
