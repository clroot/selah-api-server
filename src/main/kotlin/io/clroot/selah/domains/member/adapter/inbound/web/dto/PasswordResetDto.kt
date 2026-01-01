package io.clroot.selah.domains.member.adapter.inbound.web.dto

/**
 * 비밀번호 재설정 관련 DTO
 */
object PasswordResetDto {
    /**
     * 비밀번호 재설정 요청 (비밀번호 찾기)
     */
    data class ForgotPasswordRequest(
        val email: String,
    )

    /**
     * 비밀번호 재설정 토큰 검증 응답
     */
    data class ValidateTokenResponse(
        val valid: Boolean,
        val email: String,
    )

    /**
     * 비밀번호 재설정 요청
     */
    data class ResetPasswordRequest(
        val token: String,
        val newPassword: String,
    )
}
