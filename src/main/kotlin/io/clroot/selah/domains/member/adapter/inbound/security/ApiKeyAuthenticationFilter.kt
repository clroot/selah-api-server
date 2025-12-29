package io.clroot.selah.domains.member.adapter.inbound.security

import io.clroot.selah.common.util.HttpRequestUtils
import io.clroot.selah.domains.member.application.port.inbound.ValidateApiKeyUseCase
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * API Key 인증 필터
 *
 * X-API-Key 헤더에서 API Key를 추출하여 인증을 수행합니다.
 */
@Component
class ApiKeyAuthenticationFilter(
    private val validateApiKeyUseCase: ValidateApiKeyUseCase,
) : OncePerRequestFilter() {

    companion object {
        private const val API_KEY_HEADER = "X-API-Key"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val apiKey = request.getHeader(API_KEY_HEADER)

        if (apiKey != null) {
            val ipAddress = HttpRequestUtils.extractIpAddress(request)
            authenticateWithApiKey(apiKey, ipAddress)
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticateWithApiKey(apiKey: String, ipAddress: String?) {
        runBlocking {
            val apiKeyInfo = validateApiKeyUseCase.validate(apiKey, ipAddress) ?: return@runBlocking

            val principal = MemberPrincipal(
                memberId = apiKeyInfo.memberId,
                role = apiKeyInfo.role,
                authenticationType = MemberPrincipal.AuthenticationType.API_KEY,
            )
            val authentication = MemberAuthenticationToken(principal)
            SecurityContextHolder.getContext().authentication = authentication
        }
    }
}
