package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.security.PublicEndpoint
import io.clroot.selah.common.util.HttpRequestUtils
import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
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

        private const val MODE_LINK = "link"
    }

    /**
     * OAuth Authorization URL로 리다이렉트
     *
     * @param mode "link"면 기존 계정에 OAuth 연결, 없으면 로그인/회원가입
     */
    @GetMapping("/{provider}/authorize")
    fun authorize(
        @PathVariable provider: OAuthProvider,
        @RequestParam(required = false) mode: String?,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<Void> {
        logger.debug { "OAuth authorize request for provider: $provider, mode: $mode" }

        // CSRF 방지용 state 생성
        val state = generateState()

        // state를 쿠키에 저장 (callback에서 검증)
        sessionCookieHelper.addStateCookie(httpResponse, state)

        // mode가 link이면 mode 쿠키도 저장
        if (mode == MODE_LINK) {
            sessionCookieHelper.addModeCookie(httpResponse, MODE_LINK)
        }

        // Authorization URL 생성
        val authorizationUrl =
            oAuthCallbackUseCase.getAuthorizationUrl(
                GetAuthorizationUrlCommand(
                    provider = provider,
                    state = state,
                ),
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
        val mode = sessionCookieHelper.extractMode(httpRequest)
        logger.debug { "OAuth callback for provider: $provider, mode: $mode" }

        // Mode 쿠키 삭제
        sessionCookieHelper.clearModeCookie(httpResponse)

        // 에러 처리
        if (error != null) {
            logger.warn { "OAuth error from provider: $error" }
            return redirectToFrontendWithError("oauth_error", mode)
        }

        // State 검증
        val savedState = sessionCookieHelper.extractState(httpRequest)
        if (savedState == null || savedState != state) {
            logger.warn { "OAuth state mismatch: expected=$savedState, received=$state" }
            return redirectToFrontendWithError("state_mismatch", mode)
        }

        // State 쿠키 삭제
        sessionCookieHelper.clearStateCookie(httpResponse)

        return if (mode == MODE_LINK) {
            handleLinkCallback(provider, code)
        } else {
            handleLoginCallback(provider, code, state, httpRequest, httpResponse)
        }
    }

    /**
     * OAuth 연결 콜백 처리 (기존 계정에 OAuth 연결)
     */
    private suspend fun handleLinkCallback(
        provider: OAuthProvider,
        code: String,
    ): ResponseEntity<Void> {
        // 현재 로그인한 사용자 확인
        val memberId = SecurityUtils.getCurrentMemberId()
        if (memberId == null) {
            logger.warn { "OAuth link callback failed: user not authenticated" }
            return redirectToFrontendWithError("not_authenticated", MODE_LINK)
        }

        return try {
            oAuthCallbackUseCase.handleLinkCallback(memberId, provider, code)

            val redirectUrl = buildFrontendCallbackUrl(mode = MODE_LINK)
            logger.debug { "OAuth link callback successful, redirecting to: $redirectUrl" }

            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build()
        } catch (e: Exception) {
            logger.error(e) { "OAuth link callback failed for provider: $provider" }
            redirectToFrontendWithError("oauth_link_failed", MODE_LINK)
        }
    }

    /**
     * OAuth 로그인 콜백 처리 (로그인/회원가입)
     */
    private suspend fun handleLoginCallback(
        provider: OAuthProvider,
        code: String,
        state: String,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<Void> =
        try {
            // OAuth 콜백 처리 (token 교환, 사용자 정보 조회, 로그인)
            val result =
                oAuthCallbackUseCase.handleCallback(
                    OAuthCallbackCommand(
                        provider = provider,
                        code = code,
                        state = state,
                        userAgent = httpRequest.getHeader("User-Agent"),
                        ipAddress = HttpRequestUtils.extractIpAddress(httpRequest),
                    ),
                )

            // 세션 쿠키 설정
            sessionCookieHelper.addSessionCookie(httpResponse, result.session.token, result.session.expiresAt)

            // 프론트엔드로 리다이렉트 (성공)
            val redirectUrl = buildFrontendCallbackUrl(isNewMember = result.isNewMember)

            logger.debug { "OAuth login successful, redirecting to: $redirectUrl" }

            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build()
        } catch (e: Exception) {
            logger.error(e) { "OAuth callback failed for provider: $provider" }
            redirectToFrontendWithError("oauth_failed", null)
        }

    private fun generateState(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun buildFrontendCallbackUrl(
        isNewMember: Boolean? = null,
        mode: String? = null,
    ): String {
        val params = mutableListOf("success=true")
        if (isNewMember != null) {
            params.add("isNewMember=$isNewMember")
        }
        if (mode != null) {
            params.add("mode=$mode")
        }
        return "${oAuthProperties.frontendCallbackUrl}?${params.joinToString("&")}"
    }

    private fun redirectToFrontendWithError(
        error: String,
        mode: String?,
    ): ResponseEntity<Void> {
        val modeParam = if (mode != null) "&mode=$mode" else ""
        val redirectUrl = "${oAuthProperties.frontendCallbackUrl}?error=$error$modeParam"
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(redirectUrl))
            .build()
    }
}
