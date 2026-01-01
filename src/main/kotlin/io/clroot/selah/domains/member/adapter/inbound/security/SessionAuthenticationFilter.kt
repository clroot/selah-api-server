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
    /**
     * 비동기 디스패치 시에도 필터를 적용하도록 설정
     * doFilterInternal 내에서 ASYNC_DISPATCH 시 인증 객체를 복구함
     */
    override fun shouldNotFilterAsyncDispatch(): Boolean = false

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // ASYNC_DISPATCH인 경우: request attribute에서 Authentication 복구
        if (request.dispatcherType == jakarta.servlet.DispatcherType.ASYNC) {
            val savedAuth =
                request.getAttribute(AUTH_ATTRIBUTE_KEY) as? org.springframework.security.core.Authentication
            if (savedAuth != null) {
                SecurityContextHolder.getContext().authentication = savedAuth
            }
            filterChain.doFilter(request, response)
            return
        }

        // 이미 인증되었으면 스킵 (API Key로 인증된 경우)
        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val sessionToken = extractSessionToken(request)

        if (sessionToken != null) {
            val ipAddress = HttpRequestUtils.extractIpAddress(request)
            authenticateWithSession(sessionToken, ipAddress)
            val auth = SecurityContextHolder.getContext().authentication

            // ASYNC_DISPATCH를 위해 request attribute에 저장
            if (auth != null) {
                request.setAttribute(AUTH_ATTRIBUTE_KEY, auth)
            }
        }

        filterChain.doFilter(request, response)
    }

    companion object {
        private const val AUTH_ATTRIBUTE_KEY = "io.clroot.selah.security.AUTHENTICATION"
    }

    private fun extractSessionToken(request: HttpServletRequest): String? = request.cookies?.find { it.name == sessionCookieName }?.value

    private fun authenticateWithSession(
        sessionToken: String,
        ipAddress: String?,
    ) {
        runBlocking {
            val sessionInfo = sessionPort.findByToken(sessionToken) ?: return@runBlocking

            if (sessionInfo.isExpired()) {
                return@runBlocking
            }

            val principal =
                MemberPrincipal(
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
