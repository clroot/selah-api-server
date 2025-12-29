package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.security.PublicEndpoint
import io.clroot.selah.common.util.HttpRequestUtils
import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
import io.clroot.selah.domains.member.adapter.inbound.web.dto.*
import io.clroot.selah.domains.member.application.port.inbound.*
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.NewPassword
import io.clroot.selah.domains.member.domain.RawPassword
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalDateTime

/**
 * 인증 관련 Controller
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val registerMemberUseCase: RegisterMemberUseCase,
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    @Value($$"${selah.session.cookie-name:SELAH_SESSION}")
    private val sessionCookieName: String,
    @Value($$"${selah.session.cookie.secure:true}")
    private val cookieSecure: Boolean,
    @Value($$"${selah.session.cookie.same-site:Strict}")
    private val cookieSameSite: String,
) {

    /**
     * 이메일 회원가입
     */
    @PublicEndpoint
    @PostMapping("/register")
    suspend fun registerWithEmail(
        @RequestBody request: RegisterWithEmailRequest,
    ): ResponseEntity<ApiResponse<RegisterResponse>> {
        val member = registerMemberUseCase.registerWithEmail(
            RegisterWithEmailCommand(
                email = Email(request.email),
                nickname = request.nickname,
                password = NewPassword.from(request.password),
            )
        )

        return ResponseEntity.ok(
            ApiResponse.success(
                RegisterResponse(
                    memberId = member.id.value,
                    nickname = member.nickname,
                )
            )
        )
    }

    /**
     * 이메일 로그인
     */
    @PublicEndpoint
    @PostMapping("/login")
    suspend fun loginWithEmail(
        @RequestBody request: LoginWithEmailRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<ApiResponse<LoginResponse>> {
        val result = loginUseCase.loginWithEmail(
            LoginWithEmailCommand(
                email = Email(request.email),
                password = RawPassword(request.password),
                userAgent = httpRequest.getHeader("User-Agent"),
                ipAddress = HttpRequestUtils.extractIpAddress(httpRequest),
            )
        )

        // 세션 쿠키 설정
        addSessionCookie(httpResponse, result.session.token, result.session.expiresAt)

        return ResponseEntity.ok(ApiResponse.success(result.toResponse()))
    }

    /**
     * OAuth 로그인 (자동 회원가입 포함)
     */
    @PublicEndpoint
    @PostMapping("/oauth/login")
    suspend fun loginWithOAuth(
        @RequestBody request: LoginWithOAuthRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<ApiResponse<LoginResponse>> {
        val result = loginUseCase.loginWithOAuth(
            LoginWithOAuthCommand(
                email = Email(request.email),
                nickname = request.nickname,
                provider = request.provider,
                providerId = request.providerId,
                profileImageUrl = request.profileImageUrl,
                userAgent = httpRequest.getHeader("User-Agent"),
                ipAddress = HttpRequestUtils.extractIpAddress(httpRequest),
            )
        )

        // 세션 쿠키 설정
        addSessionCookie(httpResponse, result.session.token, result.session.expiresAt)

        return ResponseEntity.ok(ApiResponse.success(result.toResponse()))
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    suspend fun logout(
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Unit>> {
        val sessionToken = extractSessionToken(httpRequest)
        if (sessionToken != null) {
            logoutUseCase.logout(sessionToken)
        }
        clearSessionCookie(httpResponse)

        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    /**
     * 전체 로그아웃 (모든 세션에서 로그아웃)
     */
    @PostMapping("/logout/all")
    suspend fun logoutAll(
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Unit>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        logoutUseCase.logoutAll(memberId)
        clearSessionCookie(httpResponse)

        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    private fun extractSessionToken(request: HttpServletRequest): String? {
        return request.cookies?.find { it.name == sessionCookieName }?.value
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

    private fun clearSessionCookie(response: HttpServletResponse) {
        val cookie = Cookie(sessionCookieName, "").apply {
            isHttpOnly = true
            secure = cookieSecure
            path = "/"
            maxAge = 0
            setAttribute("SameSite", cookieSameSite)
        }
        response.addCookie(cookie)
    }
}
