package io.clroot.selah.domains.member.adapter.inbound.web

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

/**
 * 인증 관련 쿠키 관리 Helper
 */
@Component
class SessionCookieHelper(
    @Value($$"${selah.session.cookie-name:SELAH_SESSION}")
    private val sessionCookieName: String,
    @Value($$"${selah.session.cookie.secure:true}")
    private val cookieSecure: Boolean,
    @Value($$"${selah.session.cookie.same-site:Strict}")
    private val cookieSameSite: String,
    @Value($$"${selah.session.cookie.domain:}")
    private val cookieDomain: String,
) {
    companion object {
        private const val STATE_COOKIE_NAME = "oauth_state"
        private const val MODE_COOKIE_NAME = "oauth_mode"
        private const val OAUTH_COOKIE_PATH = "/api/v1/auth/oauth"
        private const val OAUTH_COOKIE_MAX_AGE = 600 // 10 minutes
    }

    // ===== Session Cookie =====

    /**
     * 세션 쿠키 추가
     */
    fun addSessionCookie(
        response: HttpServletResponse,
        token: String,
        expiresAt: LocalDateTime,
    ) {
        val maxAge = Duration.between(LocalDateTime.now(), expiresAt).seconds.toInt()

        val cookie =
            Cookie(sessionCookieName, token).apply {
                isHttpOnly = true
                secure = cookieSecure
                path = "/"
                this.maxAge = maxAge
                setAttribute("SameSite", cookieSameSite)
                if (cookieDomain.isNotBlank()) domain = cookieDomain
            }
        response.addCookie(cookie)
    }

    /**
     * 세션 쿠키 삭제
     */
    fun clearSessionCookie(response: HttpServletResponse) {
        val cookie =
            Cookie(sessionCookieName, "").apply {
                isHttpOnly = true
                secure = cookieSecure
                path = "/"
                maxAge = 0
                setAttribute("SameSite", cookieSameSite)
                if (cookieDomain.isNotBlank()) domain = cookieDomain
            }
        response.addCookie(cookie)
    }

    /**
     * 세션 토큰 추출
     */
    fun extractSessionToken(request: HttpServletRequest): String? = request.cookies?.find { it.name == sessionCookieName }?.value

    // ===== OAuth State Cookie =====

    /**
     * OAuth state 쿠키 추가 (CSRF 방지용)
     */
    fun addStateCookie(
        response: HttpServletResponse,
        state: String,
    ) {
        val cookie =
            Cookie(STATE_COOKIE_NAME, state).apply {
                isHttpOnly = true
                secure = cookieSecure
                path = OAUTH_COOKIE_PATH
                maxAge = OAUTH_COOKIE_MAX_AGE
                setAttribute("SameSite", cookieSameSite)
                if (cookieDomain.isNotBlank()) domain = cookieDomain
            }
        response.addCookie(cookie)
    }

    /**
     * OAuth state 쿠키 삭제
     */
    fun clearStateCookie(response: HttpServletResponse) {
        val cookie =
            Cookie(STATE_COOKIE_NAME, "").apply {
                isHttpOnly = true
                secure = cookieSecure
                path = OAUTH_COOKIE_PATH
                maxAge = 0
                setAttribute("SameSite", cookieSameSite)
                if (cookieDomain.isNotBlank()) domain = cookieDomain
            }
        response.addCookie(cookie)
    }

    /**
     * OAuth state 추출
     */
    fun extractState(request: HttpServletRequest): String? = request.cookies?.find { it.name == STATE_COOKIE_NAME }?.value

    // ===== OAuth Mode Cookie =====

    /**
     * OAuth mode 쿠키 추가 (link 모드 구분용)
     */
    fun addModeCookie(
        response: HttpServletResponse,
        mode: String,
    ) {
        val cookie =
            Cookie(MODE_COOKIE_NAME, mode).apply {
                isHttpOnly = true
                secure = cookieSecure
                path = OAUTH_COOKIE_PATH
                maxAge = OAUTH_COOKIE_MAX_AGE
                setAttribute("SameSite", cookieSameSite)
                if (cookieDomain.isNotBlank()) domain = cookieDomain
            }
        response.addCookie(cookie)
    }

    /**
     * OAuth mode 쿠키 삭제
     */
    fun clearModeCookie(response: HttpServletResponse) {
        val cookie =
            Cookie(MODE_COOKIE_NAME, "").apply {
                isHttpOnly = true
                secure = cookieSecure
                path = OAUTH_COOKIE_PATH
                maxAge = 0
                setAttribute("SameSite", cookieSameSite)
                if (cookieDomain.isNotBlank()) domain = cookieDomain
            }
        response.addCookie(cookie)
    }

    /**
     * OAuth mode 추출
     */
    fun extractMode(request: HttpServletRequest): String? = request.cookies?.find { it.name == MODE_COOKIE_NAME }?.value
}
