package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.exception.InvalidPasswordResetTokenException
import io.clroot.selah.domains.member.domain.exception.PasswordResetResendTooSoonException

/**
 * 비밀번호 재설정 UseCase
 */
interface PasswordResetUseCase {
    /**
     * 비밀번호 재설정 요청을 처리합니다.
     * 이메일이 존재하지 않아도 동일한 응답을 반환합니다. (보안)
     *
     * @param command 재설정 요청 정보
     * @throws PasswordResetResendTooSoonException 재발송 대기 시간 내인 경우
     */
    suspend fun requestPasswordReset(command: RequestPasswordResetCommand)

    /**
     * 비밀번호 재설정 토큰을 검증합니다.
     *
     * @param command 토큰 검증 요청
     * @return 검증 결과 (마스킹된 이메일 포함)
     * @throws InvalidPasswordResetTokenException 유효하지 않은 토큰인 경우
     */
    suspend fun validateResetToken(command: ValidateResetTokenCommand): ValidateResetTokenResult

    /**
     * 비밀번호를 재설정합니다.
     *
     * @param command 재설정 요청 정보
     * @throws InvalidPasswordResetTokenException 유효하지 않은 토큰인 경우
     */
    suspend fun resetPassword(command: ResetPasswordCommand)
}

/**
 * 비밀번호 재설정 요청 Command
 */
data class RequestPasswordResetCommand(
    val email: Email,
)

/**
 * 비밀번호 재설정 토큰 검증 Command
 */
data class ValidateResetTokenCommand(
    val token: String,
)

/**
 * 비밀번호 재설정 토큰 검증 결과
 */
data class ValidateResetTokenResult(
    val valid: Boolean,
    val maskedEmail: String,
)

/**
 * 비밀번호 재설정 Command
 */
data class ResetPasswordCommand(
    val token: String,
    val newPassword: String,
)
