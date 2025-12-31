package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.security.PublicEndpoint
import io.clroot.selah.common.util.HttpRequestUtils
import io.clroot.selah.domains.member.adapter.outbound.oauth.OAuthProperties
import io.clroot.selah.domains.member.application.port.inbound.GetAuthorizationUrlCommand
import io.clroot.selah.domains.member.application.port.inbound.OAuthCallbackCommand
import io.clroot.selah.domains.member.application.port.inbound.OAuthCallbackUseCase
import io.clroot.selah.domains.member.domain.OAuthProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime
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
    @Value($$"${selah.session.cookie-name:SELAH_SESSION}")
    private val sessionCookieName: String,
    @Value($$"${selah.session.cookie.secure:true}")
    private val cookieSecure: Boolean,
    @Value($$"${selah.session.cookie.same-site:Strict}")
    private val cookieSameSite: String,
) {

    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}

        private const val STATE_COOKIE_NAME = "oauth_state"
        private const val STATE_COOKIE_MAX_AGE = 600 // 10 minutes
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
        val stateCookie = Cookie(STATE_COOKIE_NAME, state).apply {
            isHttpOnly = true
            secure = cookieSecure
            path = "/api/v1/auth/oauth"
            maxAge = STATE_COOKIE_MAX_AGE
            setAttribute("SameSite", cookieSameSite)
        }
        httpResponse.addCookie(stateCookie)

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
        val savedState = httpRequest.cookies?.find { it.name == STATE_COOKIE_NAME }?.value
        if (savedState == null || savedState != state) {
            logger.warn { "OAuth state mismatch: expected=$savedState, received=$state" }
            return redirectToFrontendWithError("state_mismatch")
        }

        // State 쿠키 삭제
        clearStateCookie(httpResponse)

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
            addSessionCookie(httpResponse, result.session.token, result.session.expiresAt)

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

    private fun addSessionCookie(
        response: HttpServletResponse,
        token: String,
        expiresAt: LocalDateTime,
    ) {
        val maxAge = Duration.between(LocalDateTime.now(), expiresAt).seconds.toInt()

        val cookie = Cookie(sessionCookieName, token).apply {
            isHttpOnly = true
            secure = cookieSecure
            path = "/"
            this.maxAge = maxAge
            setAttribute("SameSite", cookieSameSite)
        }
        response.addCookie(cookie)
    }

    private fun clearStateCookie(response: HttpServletResponse) {
        val cookie = Cookie(STATE_COOKIE_NAME, "").apply {
            isHttpOnly = true
            secure = cookieSecure
            path = "/api/v1/auth/oauth"
            maxAge = 0
            setAttribute("SameSite", cookieSameSite)
        }
        response.addCookie(cookie)
    }
}
