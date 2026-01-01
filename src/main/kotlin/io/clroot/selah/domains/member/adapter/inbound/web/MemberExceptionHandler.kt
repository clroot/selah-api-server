package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.response.ErrorResponse
import io.clroot.selah.domains.member.domain.exception.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Member 도메인 예외 핸들러
 */
@RestControllerAdvice(basePackages = ["io.clroot.selah.domains.member"])
class MemberExceptionHandler {
    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    @ExceptionHandler(MemberException::class)
    fun handleMemberException(ex: MemberException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug { "Member exception: ${ex.code} - ${ex.message}" }

        val (status, message) =
            when (ex) {
                // 인증/인가
                is InvalidCredentialsException -> HttpStatus.UNAUTHORIZED to "이메일 또는 비밀번호가 올바르지 않습니다"
                is SessionExpiredException -> HttpStatus.UNAUTHORIZED to "세션이 만료되었습니다"
                is InvalidSessionException -> HttpStatus.UNAUTHORIZED to "유효하지 않은 세션입니다"
                is InvalidApiKeyException -> HttpStatus.UNAUTHORIZED to "유효하지 않은 API 키입니다"

                // 회원
                is MemberNotFoundException -> HttpStatus.NOT_FOUND to "회원을 찾을 수 없습니다"
                is EmailAlreadyExistsException -> HttpStatus.CONFLICT to "이미 사용 중인 이메일입니다"

                // 이메일 인증
                is EmailNotVerifiedException -> HttpStatus.FORBIDDEN to "이메일 인증이 필요합니다"
                is EmailAlreadyVerifiedException -> HttpStatus.CONFLICT to "이미 인증된 이메일입니다"
                is InvalidEmailVerificationTokenException -> HttpStatus.BAD_REQUEST to "유효하지 않은 인증 토큰입니다"
                is EmailVerificationTokenExpiredException -> HttpStatus.GONE to "인증 토큰이 만료되었습니다"
                is EmailVerificationResendTooSoonException -> HttpStatus.TOO_MANY_REQUESTS to "${ex.remainingSeconds}초 후에 다시 시도해주세요"

                // 비밀번호 재설정
                is InvalidPasswordResetTokenException -> HttpStatus.BAD_REQUEST to "유효하지 않은 비밀번호 재설정 토큰입니다"
                is PasswordResetResendTooSoonException -> HttpStatus.TOO_MANY_REQUESTS to "${ex.remainingSeconds}초 후에 다시 시도해주세요"

                // 비밀번호 변경/설정
                is PasswordNotSetException -> HttpStatus.BAD_REQUEST to "비밀번호가 설정되지 않았습니다. 비밀번호 설정을 먼저 진행해주세요."
                is PasswordAlreadySetException -> HttpStatus.CONFLICT to "이미 비밀번호가 설정되어 있습니다. 비밀번호 변경을 이용해주세요."

                // OAuth
                is OAuthProviderAlreadyConnectedException -> HttpStatus.CONFLICT to "이미 연결된 소셜 계정입니다"
                is OAuthProviderNotConnectedException -> HttpStatus.NOT_FOUND to "연결되지 않은 소셜 계정입니다"
                is OAuthTokenValidationFailedException -> HttpStatus.UNAUTHORIZED to "소셜 계정 인증에 실패했습니다"
                is OAuthTokenExchangeFailedException -> HttpStatus.BAD_GATEWAY to "소셜 계정 인증 토큰 교환에 실패했습니다"
                is OAuthStateValidationFailedException -> HttpStatus.BAD_REQUEST to "잘못된 인증 요청입니다"
                is OAuthAlreadyLinkedToAnotherMemberException -> HttpStatus.CONFLICT to "이미 다른 계정에 연결된 소셜 계정입니다"
                is CannotDisconnectLastLoginMethodException ->
                    HttpStatus.BAD_REQUEST to
                        "마지막 로그인 방법은 해제할 수 없습니다. 비밀번호를 설정하거나 다른 소셜 계정을 연결해주세요."

                // 암호화 설정
                is EncryptionSettingsNotFoundException -> HttpStatus.NOT_FOUND to "암호화 설정을 찾을 수 없습니다"
                is EncryptionAlreadySetupException -> HttpStatus.CONFLICT to "이미 암호화가 설정되어 있습니다"
                is ServerKeyNotFoundException -> HttpStatus.NOT_FOUND to "서버 키를 찾을 수 없습니다"
            }

        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(ErrorResponse(ex.code, message)))
    }
}
