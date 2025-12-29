package io.clroot.selah.domains.member.adapter.inbound.security

import io.clroot.selah.common.util.HttpRequestUtils
import io.clroot.selah.domains.member.application.port.outbound.SessionPort
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 세션 인증 필터
 *
 * 쿠키에서 세션 토큰을 추출하여 인증을 수행합니다.
 */
@Component
class SessionAuthenticationFilter(
    private val sessionPort: SessionPort,
    @Value($$"${selah.session.cookie-name:SELAH_SESSION}")
    private val sessionCookieName: String,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // 이미 인증되었으면 스킵 (API Key로 인증된 경우)
        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val sessionToken = extractSessionToken(request)
        if (sessionToken != null) {
            val ipAddress = HttpRequestUtils.extractIpAddress(request)
            authenticateWithSession(sessionToken, ipAddress)
        }

        filterChain.doFilter(request, response)
    }

    private fun extractSessionToken(request: HttpServletRequest): String? {
        return request.cookies?.find { it.name == sessionCookieName }?.value
    }

    private fun authenticateWithSession(sessionToken: String, ipAddress: String?) {
        runBlocking {
            val sessionInfo = sessionPort.findByToken(sessionToken) ?: return@runBlocking

            if (sessionInfo.isExpired()) {
                return@runBlocking
            }

            val principal = MemberPrincipal(
                memberId = sessionInfo.memberId,
                role = sessionInfo.role,
                authenticationType = MemberPrincipal.AuthenticationType.SESSION,
            )
            val authentication = MemberAuthenticationToken(principal)
            SecurityContextHolder.getContext().authentication = authentication

            // 세션 연장 및 마지막 접근 IP 업데이트 (Sliding Session)
            sessionPort.extendExpiry(sessionToken, ipAddress)
        }
    }
}
