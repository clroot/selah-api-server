package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.security.PublicEndpoint
import io.clroot.selah.common.util.HttpRequestUtils
import io.clroot.selah.domains.member.adapter.outbound.oauth.OAuthProperties
import io.clroot.selah.domains.member.application.port.inbound.GetAuthorizationUrlCommand
import io.clroot.selah.domains.member.application.port.inbound.OAuthCallbackCommand
import io.clroot.selah.domains.member.application.port.inbound.OAuthCallbackUseCase
import io.clroot.selah.domains.member.domain.OAuthProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.security.SecureRandom
import java.util.*

/**
 * OAuth 인증 Controller
 */
@PublicEndpoint
@RestController
@RequestMapping("/api/v1/auth/oauth")
class OAuthController(
    private val oAuthCallbackUseCase: OAuthCallbackUseCase,
    private val oAuthProperties: OAuthProperties,
    private val sessionCookieHelper: SessionCookieHelper,
) {

    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    /**
     * OAuth Authorization URL로 리다이렉트
     */
    @GetMapping("/{provider}/authorize")
    fun authorize(
        @PathVariable provider: OAuthProvider,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<Void> {
        logger.debug { "OAuth authorize request for provider: $provider" }

        // CSRF 방지용 state 생성
        val state = generateState()

        // state를 쿠키에 저장 (callback에서 검증)
        sessionCookieHelper.addStateCookie(httpResponse, state)

        // Authorization URL 생성
        val authorizationUrl = oAuthCallbackUseCase.getAuthorizationUrl(
            GetAuthorizationUrlCommand(
                provider = provider,
                state = state,
            )
        )

        logger.debug { "Redirecting to OAuth provider: $authorizationUrl" }

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(authorizationUrl))
            .build()
    }

    /**
     * OAuth Callback 처리
     */
    @GetMapping("/{provider}/callback")
    suspend fun callback(
        @PathVariable provider: OAuthProvider,
        @RequestParam code: String,
        @RequestParam state: String,
        @RequestParam(required = false) error: String?,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<Void> {
        logger.debug { "OAuth callback for provider: $provider" }

        // 에러 처리
        if (error != null) {
            logger.warn { "OAuth error from provider: $error" }
            return redirectToFrontendWithError("oauth_error")
        }

        // State 검증
        val savedState = sessionCookieHelper.extractState(httpRequest)
        if (savedState == null || savedState != state) {
            logger.warn { "OAuth state mismatch: expected=$savedState, received=$state" }
            return redirectToFrontendWithError("state_mismatch")
        }

        // State 쿠키 삭제
        sessionCookieHelper.clearStateCookie(httpResponse)

        return try {
            // OAuth 콜백 처리 (token 교환, 사용자 정보 조회, 로그인)
            val result = oAuthCallbackUseCase.handleCallback(
                OAuthCallbackCommand(
                    provider = provider,
                    code = code,
                    state = state,
                    userAgent = httpRequest.getHeader("User-Agent"),
                    ipAddress = HttpRequestUtils.extractIpAddress(httpRequest),
                )
            )

            // 세션 쿠키 설정
            sessionCookieHelper.addSessionCookie(httpResponse, result.session.token, result.session.expiresAt)

            // 프론트엔드로 리다이렉트 (성공)
            val redirectUrl = buildFrontendCallbackUrl(
                isNewMember = result.isNewMember,
            )

            logger.debug { "OAuth login successful, redirecting to: $redirectUrl" }

            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build()
        } catch (e: Exception) {
            logger.error(e) { "OAuth callback failed for provider: $provider" }
            redirectToFrontendWithError("oauth_failed")
        }
    }

    private fun generateState(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun buildFrontendCallbackUrl(isNewMember: Boolean): String {
        return "${oAuthProperties.frontendCallbackUrl}?success=true&isNewMember=$isNewMember"
    }

    private fun redirectToFrontendWithError(error: String): ResponseEntity<Void> {
        val redirectUrl = "${oAuthProperties.frontendCallbackUrl}?error=$error"
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(redirectUrl))
            .build()
    }
}
