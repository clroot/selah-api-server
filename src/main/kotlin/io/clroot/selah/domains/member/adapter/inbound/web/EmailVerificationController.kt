package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.security.PublicEndpoint
import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
import io.clroot.selah.domains.member.adapter.inbound.web.dto.EmailVerificationDto
import io.clroot.selah.domains.member.application.port.inbound.EmailVerificationUseCase
import io.clroot.selah.domains.member.application.port.inbound.SendVerificationEmailCommand
import io.clroot.selah.domains.member.application.port.inbound.VerifyEmailCommand
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 이메일 인증 Controller
 */
@RestController
@RequestMapping("/api/v1/auth/email")
class EmailVerificationController(
    private val emailVerificationUseCase: EmailVerificationUseCase,
) {

    /**
     * 이메일 인증 메일 발송 (재발송)
     * 로그인된 사용자가 자신의 이메일 인증을 요청합니다.
     */
    @PostMapping("/send-verification")
    suspend fun sendVerificationEmail(): ResponseEntity<ApiResponse<Unit>> {
        val memberId = SecurityUtils.requireCurrentMemberId()

        emailVerificationUseCase.sendVerificationEmail(
            SendVerificationEmailCommand(memberId = memberId)
        )

        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    /**
     * 이메일 인증 완료
     * 토큰은 이메일 링크를 통해 전달받습니다.
     */
    @PublicEndpoint
    @PostMapping("/verify")
    suspend fun verifyEmail(
        @RequestBody request: EmailVerificationDto.VerifyRequest,
    ): ResponseEntity<ApiResponse<EmailVerificationDto.VerifyResponse>> {
        val member = emailVerificationUseCase.verifyEmail(
            VerifyEmailCommand(token = request.token)
        )

        return ResponseEntity.ok(
            ApiResponse.success(
                EmailVerificationDto.VerifyResponse(
                    memberId = member.id!!.value,
                    email = member.email.value,
                    emailVerified = member.emailVerified,
                )
            )
        )
    }
}
