package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.Email

/**
 * 이메일 발송을 위한 Outbound Port
 */
interface SendEmailPort {
    /**
     * 이메일 인증 메일을 발송합니다.
     *
     * @param to 수신자 이메일
     * @param nickname 수신자 닉네임
     * @param verificationToken 인증 토큰 (URL에 포함됨)
     */
    suspend fun sendVerificationEmail(
        to: Email,
        nickname: String,
        verificationToken: String,
    )
}
