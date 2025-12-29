package io.clroot.selah.domains.member.adapter.inbound.web.dto

import io.clroot.selah.domains.member.application.port.inbound.LoginResult
import io.clroot.selah.domains.member.domain.OAuthProvider
import java.time.LocalDateTime

// === Request DTOs ===

/**
 * 이메일 회원가입 요청
 */
data class RegisterWithEmailRequest(
    val email: String,
    val nickname: String,
    val password: String,
)

/**
 * 이메일 로그인 요청
 */
data class LoginWithEmailRequest(
    val email: String,
    val password: String,
)

/**
 * OAuth 로그인 요청
 */
data class LoginWithOAuthRequest(
    val email: String,
    val nickname: String,
    val provider: OAuthProvider,
    val providerId: String,
    val profileImageUrl: String? = null,
)

// === Response DTOs ===

/**
 * 로그인 응답
 */
data class LoginResponse(
    val memberId: String,
    val nickname: String,
    val isNewMember: Boolean,
    val expiresAt: LocalDateTime,
)

/**
 * 회원가입 응답
 */
data class RegisterResponse(
    val memberId: String,
    val nickname: String,
)

// === Extension Functions ===

fun LoginResult.toResponse(): LoginResponse = LoginResponse(
    memberId = memberId.value,
    nickname = nickname,
    isNewMember = isNewMember,
    expiresAt = session.expiresAt,
)
