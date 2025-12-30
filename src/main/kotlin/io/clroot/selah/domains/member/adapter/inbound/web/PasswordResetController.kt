package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.security.PublicEndpoint
import io.clroot.selah.domains.member.adapter.inbound.web.dto.PasswordResetDto
import io.clroot.selah.domains.member.application.port.inbound.PasswordResetUseCase
import io.clroot.selah.domains.member.application.port.inbound.RequestPasswordResetCommand
import io.clroot.selah.domains.member.application.port.inbound.ResetPasswordCommand
import io.clroot.selah.domains.member.application.port.inbound.ValidateResetTokenCommand
import io.clroot.selah.domains.member.domain.Email
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 비밀번호 재설정 Controller
 */
@PublicEndpoint
@RestController
@RequestMapping("/api/v1/auth/password")
class PasswordResetController(
    private val passwordResetUseCase: PasswordResetUseCase,
) {

    /**
     * 비밀번호 재설정 요청 (비밀번호 찾기)
     * 이메일 존재 여부와 관계없이 항상 성공 응답을 반환합니다. (보안)
     */
    @PostMapping("/forgot")
    suspend fun forgotPassword(
        @RequestBody request: PasswordResetDto.ForgotPasswordRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        passwordResetUseCase.requestPasswordReset(
            RequestPasswordResetCommand(email = Email(request.email))
        )

        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    /**
     * 비밀번호 재설정 토큰 검증
     */
    @GetMapping("/reset/validate")
    suspend fun validateResetToken(
        @RequestParam token: String,
    ): ResponseEntity<ApiResponse<PasswordResetDto.ValidateTokenResponse>> {
        val result = passwordResetUseCase.validateResetToken(
            ValidateResetTokenCommand(token = token)
        )

        return ResponseEntity.ok(
            ApiResponse.success(
                PasswordResetDto.ValidateTokenResponse(
                    valid = result.valid,
                    email = result.maskedEmail,
                )
            )
        )
    }

    /**
     * 비밀번호 재설정
     */
    @PostMapping("/reset")
    suspend fun resetPassword(
        @RequestBody request: PasswordResetDto.ResetPasswordRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        passwordResetUseCase.resetPassword(
            ResetPasswordCommand(
                token = request.token,
                newPassword = request.newPassword,
            )
        )

        return ResponseEntity.ok(ApiResponse.success(Unit))
    }
}
