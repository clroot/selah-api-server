package io.clroot.selah.domains.member.adapter.inbound.web.dto

/**
 * 이메일 인증 관련 DTO
 */
object EmailVerificationDto {

    /**
     * 이메일 인증 요청
     */
    data class VerifyRequest(
        val token: String,
    )

    /**
     * 이메일 인증 응답
     */
    data class VerifyResponse(
        val memberId: String,
        val email: String,
        val emailVerified: Boolean,
    )
}
